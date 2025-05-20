package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.HfDirectoryWalker
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.convert.toNSURL
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.moved
import io.github.vinceglb.filekit.utils.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.Path
import okio.Path.Companion.toPath
import platform.CoreML.MLBatchProviderProtocol
import platform.CoreML.MLFeatureProviderProtocol
import platform.CoreML.MLFeatureValue
import platform.CoreML.MLModelConfiguration
import platform.CoreML.MLMultiArray
import platform.CoreML.MLMultiArrayDataTypeFloat16
import platform.CoreML.compileModelAtURL
import platform.CoreML.create
import platform.CoreML.objectAtIndexedSubscript
import platform.CoreML.setObject
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL
import platform.NaturalLanguage.NLLanguageRecognizer
import platform.darwin.NSInteger
import platform.darwin.NSObject
import tokenizers.Encoding
import tokenizers.Tokenizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias CoreMLModel = platform.CoreML.MLModel

actual class LanguageIdentifier {
    actual suspend fun getLanguage(text: String): Language {
        val recognizer = NLLanguageRecognizer()
        recognizer.processString(text)
        return when (recognizer.dominantLanguage) {
            "zh-Hans", "zh-Hant" -> Language.Chinese
            "en" -> Language.English
            "de" -> Language.German
            "es" -> Language.Spanish
            null -> Language.Default
            else -> Language.World
        }
    }
}

typealias MLFeatureResultProducer = (MLFeatureProviderProtocol) -> FloatArray

interface MLFeatureProviderProducer {
    fun one(frame: Frame): Map<String, MLFeatureValue>
    fun <T : Frame> many(frames: List<T>): Map<String, List<MLFeatureValue>>
}

class CoreMLInference(
    override val model: MlModel,
    val ml: CoreMLModel,
    val providerProducer: MLFeatureProviderProducer,
    val resultProducer: MLFeatureResultProducer,
) : FrameEmbeddingInference {

    override suspend fun getEmbeddings(frame: Frame): FloatArray = suspendCoroutine { c ->
        ml.predictionFromFeatures(FeatureProviderAdapter(providerProducer, listOf(frame))) { p, e ->
            if (e != null) {
                c.resumeWithException(IllegalStateException(e.localizedDescription))
            } else {
                c.resume(resultProducer(p!!))
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun <T : Frame> getEmbeddings(frames: List<T>): List<FloatArray> =
        suspendCoroutine { c ->
            memScoped {
                val errPtr = allocPointerTo<ObjCObjectVar<NSError?>>()
                val predictions = ml.predictionsFromBatch(
                    FeatureProviderAdapter(providerProducer, frames),
                    errPtr.value
                )
                val error = errPtr.pointed?.value
                if (error != null) {
                    c.resumeWithException(IllegalStateException(error.localizedDescription))
                } else if (predictions != null) {
                    c.resume((0 until predictions.count).map {
                        resultProducer(predictions.featuresAtIndex(it))
                    })
                }
            }
        }

    override fun close() {
        // noop
    }

    @OptIn(ExperimentalForeignApi::class)
    class FeatureProviderAdapter<T : Frame>(
        val producer: MLFeatureProviderProducer,
        val frames: List<T>,
    ) :
        NSObject(), MLBatchProviderProtocol, MLFeatureProviderProtocol {
        private val features: Map<String, List<MLFeatureValue>> by lazy {
            if (frames.size == 1) {
                producer.one(frames.first())
                    .mapValues { (_, value) ->
                        listOf(value)
                    }
            } else if (frames.size > 1) {
                producer.many(frames)
            } else {
                emptyMap()
            }
        }

        override fun count(): NSInteger = frames.size.convert()

        override fun featuresAtIndex(index: NSInteger): MLFeatureProviderProtocol {
            return object : NSObject(), MLFeatureProviderProtocol {
                override fun featureValueForName(featureName: String): MLFeatureValue? =
                    features[featureName]?.get(index.convert())

                override fun featureNames(): Set<String> = features.keys
            }
        }

        override fun featureValueForName(featureName: String): MLFeatureValue? {
            return features[featureName]?.first()
        }

        override fun featureNames(): Set<String> = features.keys
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class JinaModelProviderProducer(
    val sequenceLength: Int = 128,
    val tokenizer: Tokenizer,
) : MLFeatureProviderProducer {
    private fun createSequenceMultiArray(dataType: Long = MLMultiArrayDataTypeFloat16): MLMultiArray =
        memScoped {
            val errPtr = allocPointerTo<ObjCObjectVar<NSError?>>()
            val array = MLMultiArray.create(
                shape = listOf(1, sequenceLength),
                dataType = dataType,
                error = errPtr.value
            )
            errPtr.value?.let {
                throw IllegalStateException(it.pointed.value!!.localizedDescription)
            }
            array ?: throw NullPointerException("MLMultiArray")
        }

    private fun Encoding.getIdMLArray(): MLMultiArray =
        createSequenceMultiArray().apply {
            ids.forEachIndexed { idx, it ->
                setObject(NSNumber(it.toInt()), atIndexedSubscript = idx.toLong())
            }
        }

    private fun Encoding.getAttentionMaskMLArray(): MLMultiArray =
        createSequenceMultiArray().apply {
            attentionMask.forEachIndexed { idx, it ->
                setObject(NSNumber(it.toInt()), atIndexedSubscript = idx.toLong())
            }
            (attentionMask.size until sequenceLength).forEach { idx ->
                setObject(NSNumber(0), atIndexedSubscript = idx.toLong())
            }
        }

    override fun one(frame: Frame): Map<String, MLFeatureValue> =
        when (frame) {
            is Frame.Text -> {
                val tokens = tokenizer.encode(frame.textFrame.content, withSpecialTokens = true)
                val ids = tokens.getIdMLArray()
                val attentionMask = tokens.getAttentionMaskMLArray()
                mapOf(
                    "input_ids" to MLFeatureValue.featureValueWithMultiArray(ids),
                    "attention_mask" to MLFeatureValue.featureValueWithMultiArray(attentionMask)
                )
            }

            is Frame.Options -> {
                throw UnsupportedOperationException("Embedding inference is not supported on nested frames.")
            }

            else -> throw UnsupportedOperationException("Embedding inference on ${frame::class.simpleName} is not supported.")
        }

    override fun <T : Frame> many(frames: List<T>): Map<String, List<MLFeatureValue>> =
        buildMap {
            if (frames.isEmpty()) {
                set("input_ids", emptyList())
                set("attention_mask", emptyList())
                return@buildMap
            }
            when (frames[0]) {
                is Frame.Text -> {
                    val textEncodings =
                        tokenizer.encode(
                            frames.map { (it as Frame.Text).textFrame.content },
                            withSpecialTokens = true
                        )
                    val ids =
                        textEncodings.map { MLFeatureValue.featureValueWithMultiArray(it.getIdMLArray()) }
                    val attentionMasks =
                        textEncodings.map { MLFeatureValue.featureValueWithMultiArray(it.getAttentionMaskMLArray()) }
                    set("input_ids", ids)
                    set("attention_mask", attentionMasks)
                }

                else -> {
                    throw UnsupportedOperationException("Embedding inference on ${frames[0]::class.simpleName} is not supported.")
                }
            }
        }
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private class SingleOutputResultProducer(val featureName: String) : MLFeatureResultProducer by {
    val feature = it.featureValueForName(featureName)!!
    val value = feature.multiArrayValue!!
    val shape = value.shape as List<NSNumber>
    assert(shape.size == 2 && (shape.first() as NSNumber).intValue == 1) { "shape error, (1, n) expected, got (${value.shape.joinToString()})" }
    assert(value.dataType == MLMultiArrayDataTypeFloat16) { "type error" }
    FloatArray(shape[1].intValue) { idx -> value.objectAtIndexedSubscript(idx.toLong()).floatValue }
}

actual suspend fun createFrameEmbeddingInference(model: MlModel): Flow<InferenceModelState> = flow {
    when (model) {
        JinaV2SmallEn -> {
            val modelUrl = NSBundle.mainBundle.URLForResource("CoreML/JinaV2EnSmall", "mlmodelc")!!
            val ml = CoreMLModel(modelUrl)
            val tokenizer = Tokenizer(
                NSBundle.mainBundle.URLForResource(
                    "CoreML/JinaV2EnSmallTokenizer",
                    "json"
                )!!
            )

            emit(
                InferenceModelState.Complete(
                    CoreMLInference(
                        model = model,
                        ml = ml,
                        providerProducer = JinaModelProviderProducer(
                            sequenceLength = model.sequenceLength ?: 512,
                            tokenizer = tokenizer
                        ),
                        resultProducer = SingleOutputResultProducer(featureName = "pooler_output")
                    )
                )
            )
        }

        else -> {
            val platform = getPlatform()
            val fs = platform.filesystem
            val coreMlFolder = platform.resourcePath.resolve("CoreML")

            if (!fs.exists(coreMlFolder)) {
                fs.createDirectories(coreMlFolder, mustCreate = true)
            }

            val modelFileName = model.hfId.replace('/', '-')
            val localMlModelC = coreMlFolder.resolve("$modelFileName.mlmodelc")
            val localTokenizer = coreMlFolder.resolve("$modelFileName-tokenizer.json")

            if (!fs.exists(localMlModelC)) {
                val cacheFolder = (NSSearchPathForDirectoriesInDomains(
                    NSCachesDirectory,
                    NSUserDomainMask,
                    true
                ).first() as String).toPath()
                val localMlPackage = cacheFolder.resolve("$modelFileName.mlpackage")

                if (fs.exists(localMlPackage)) {
                    // assume the file's integrity
                    compileModel(localMlPackage, localMlModelC)
                } else {
                    // download from hugging face
                    val modelRoot = "CoreML/model.mlpackage"
                    downloadRecursively(
                        HfDirectoryWalker(
                            model.hfId,
                            path = modelRoot
                        ).moved(modelRoot),
                        localMlPackage
                    ).mapToGroup().collect {
                        when (it) {
                            GroupedDownloadState.Completed -> {
                                compileModel(localMlPackage, localMlModelC)
                            }

                            GroupedDownloadState.Preparing -> {
                                emit(InferenceModelState.PrepareDownload)
                            }

                            is GroupedDownloadState.Progress -> {
                                emit(
                                    InferenceModelState.Download(
                                        it.ongoingDownloads,
                                        it.completedFiles,
                                        it.overallProgress
                                    )
                                )
                            }
                        }
                    }
                }

                fs.deleteRecursively(localMlPackage)
            }

            if (!fs.exists(localTokenizer)) {
                val file =
                    HfDirectoryWalker(repoId = model.hfId).getDownloadableFile("tokenizer.json")
                emit(InferenceModelState.PrepareDownload)

                downloadSingle(file, localTokenizer).collect {
                    when (it) {
                        is DownloadState.Downloading -> {
                            emit(
                                InferenceModelState.Download(
                                    mapOf(it.file to it.progress),
                                    emptyList(),
                                    it.progress
                                )
                            )
                        }

                        else -> {}
                    }
                }
            }

            val tokenizer = Tokenizer(localTokenizer.toNSURL())
            val ml = CoreMLModel(localMlModelC.toNSURL())

            emit(InferenceModelState.Complete(CoreMLInference(
                model = model,
                ml = ml,
                providerProducer = JinaModelProviderProducer(
                    sequenceLength = model.sequenceLength ?: 512,
                    tokenizer = tokenizer
                ),
                resultProducer = SingleOutputResultProducer("pooler_output")
            )))
        }
    }
}

private fun Tokenizer(loadContentOf: NSURL): Tokenizer =
    NSData.dataWithContentsOfURL(loadContentOf)!!
        .toByteArray()
        .let(Tokenizer::fromBytes)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private suspend fun compileModel(modelPath: Path, storePath: Path): Unit = suspendCoroutine { c ->
    CoreMLModel.compileModelAtURL(modelPath.toNSURL()) { modelUrl, err ->
        if (err != null) {
            c.resumeWithException(IllegalStateException(err.localizedDescription))
            return@compileModelAtURL
        }

        val storeUrl = storePath.toNSURL()

        memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            NSFileManager.defaultManager.moveItemAtURL(
                srcURL = modelUrl!!,
                toURL = storeUrl,
                error = err.ptr
            )
            err.value?.localizedDescription?.let { error(it) }

            storeUrl.setResourceValue(false, NSURLIsExcludedFromBackupKey, err.ptr)
            err.value?.localizedDescription?.let { println("Error marking compiled model as excluded from backup: $it") }
        }

        c.resume(Unit)
    }
}

private suspend fun CoreMLModel(loadContentOf: NSURL): CoreMLModel = suspendCoroutine { c ->
    CoreMLModel.loadContentsOfURL(loadContentOf, MLModelConfiguration()) { m, e ->
        if (e != null) {
            c.resumeWithException(IllegalStateException(e.localizedDescription))
        } else {
            c.resume(m!!)
        }
    }
}

