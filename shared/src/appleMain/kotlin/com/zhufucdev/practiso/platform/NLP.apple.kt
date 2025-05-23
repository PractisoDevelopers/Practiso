package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.HfDirectoryWalker
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.ListedDirectoryWalker
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import okio.Path
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
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
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

actual suspend fun createFrameEmbeddingInference(model: MlModel): Flow<InferenceModelState> = flow {
    val resultProducer = SingleOutputResultProducer(featureName = "pooler_output")
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
                        providerProducer = BertModelProviderProducer(
                            sequenceLength = model.sequenceLength ?: 512,
                            tokenizer = tokenizer
                        ),
                        resultProducer = resultProducer
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

            if (!fs.exists(localMlModelC) || !fs.exists(localTokenizer)) {
                val localMlPackage = coreMlFolder.resolve("$modelFileName.mlpackage")

                val filesToDownload = buildList {
                    if (!fs.exists(localMlModelC) && !fs.exists(localMlPackage)) {
                        val modelRoot = "CoreML/model.mlpackage"
                        addAll(
                            HfDirectoryWalker(
                                model.hfId,
                                path = modelRoot
                            ).moved(modelRoot)
                                .files
                                .map { it.copy(name = localMlPackage.name + "/" + it.name) }
                                .toList()
                        )
                    }
                    if (!fs.exists(localTokenizer)) {
                        val file =
                            HfDirectoryWalker(repoId = model.hfId).getDownloadableFile("tokenizer.json")
                        add(file.copy(name = localTokenizer.name))
                    }
                }

                if (!fs.exists(localMlModelC) && fs.exists(localMlPackage)) {
                    // assume the file's integrity
                    compileModel(localMlPackage, localMlModelC)
                    fs.deleteRecursively(localMlPackage)
                }

                if (filesToDownload.isNotEmpty()) {
                    // download from hugging face
                    downloadRecursively(
                        ListedDirectoryWalker(filesToDownload, model.hfId),
                        coreMlFolder
                    ).mapToGroup().collect { download ->
                        when (download) {
                            GroupedDownloadState.Completed -> {
                                if (fs.exists(localMlPackage)) {
                                    compileModel(localMlPackage, localMlModelC)
                                    fs.deleteRecursively(localMlPackage)
                                }
                            }

                            is GroupedDownloadState.Planed -> {
                                emit(
                                    InferenceModelState.PlanDownload(
                                        download.filesToDownload,
                                        download.configure
                                    )
                                )
                            }

                            is GroupedDownloadState.Progress -> {
                                emit(
                                    InferenceModelState.Download(
                                        download.ongoingDownloads,
                                        download.completedFiles,
                                        download.overallProgress
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val tokenizer = Tokenizer(localTokenizer.toNSURL())
            val ml = CoreMLModel(localMlModelC.toNSURL())

            emit(
                InferenceModelState.Complete(
                    CoreMLInference(
                        model = model,
                        ml = ml,
                        providerProducer = BertModelProviderProducer(
                            sequenceLength = model.sequenceLength ?: 512,
                            tokenizer = tokenizer
                        ),
                        resultProducer = resultProducer
                    )
                )
            )
        }
    }
}

typealias MLFeatureResultProducer = (modelOutput: MLFeatureProviderProtocol, input: MLFeatureProviderProtocol) -> FloatArray

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
        val input = FeatureProviderAdapter(providerProducer, listOf(frame))
        ml.predictionFromFeatures(input) { p, e ->
            if (e != null) {
                c.resumeWithException(IllegalStateException(e.localizedDescription))
            } else {
                c.resume(resultProducer(p!!, input))
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun <T : Frame> getEmbeddings(frames: List<T>): List<FloatArray> =
        suspendCoroutine { c ->
            memScoped {
                val errPtr = allocPointerTo<ObjCObjectVar<NSError?>>()
                val input = FeatureProviderAdapter(providerProducer, frames)
                val predictions = ml.predictionsFromBatch(
                    input,
                    errPtr.value
                )
                val error = errPtr.pointed?.value
                if (error != null) {
                    c.resumeWithException(IllegalStateException(error.localizedDescription))
                } else if (predictions != null) {
                    c.resume((0 until predictions.count).map {
                        resultProducer(predictions.featuresAtIndex(it), input.featuresAtIndex(it))
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
private class BertModelProviderProducer(
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
private class SingleOutputResultProducer(val featureName: String) :
    MLFeatureResultProducer by { output, _ ->
        val feature = output.featureValueForName(featureName)!!
        val value = feature.multiArrayValue!!
        val shape = value.shape as List<NSNumber>
        assert(shape.size == 2 && (shape.first() as NSNumber).intValue == 1) { "shape error, (1, n) expected, got (${value.shape.joinToString()})" }
        assert(value.dataType == MLMultiArrayDataTypeFloat16) { "type error" }
        FloatArray(shape[1].intValue) { idx -> value.objectAtIndexedSubscript(idx.toLong()).floatValue }
    }

private class MeanPoolingResultProducer(
    val hiddenStateFeature: String,
    val attentionMaskFeature: String,
) : MLFeatureResultProducer by { output, input ->
    val attentionMask = input.featureValueForName(attentionMaskFeature)!!.multiArrayValue!!.let {
        var started = false
        var attentionLen = 0
        while (attentionLen < it.shape[1] as Int) {
            val v: Int = it.objectAtIndexedSubscript(attentionLen.toLong()).intValue
            if (started && v == 0) {
                break
            } else if (!started && v == 1) {
                started = true
            }
            attentionLen++
        }
        IntArray(attentionLen) { idx ->
            it.objectAtIndexedSubscript(idx.toLong()).intValue
        }
    }
    val attentionCount = attentionMask.count { it == 1 }

    val hiddenState = output.featureValueForName(hiddenStateFeature)!!.multiArrayValue!!
    val seqLen = (hiddenState.shape[2] as Long).toInt()
    var result = FloatArray(seqLen)
    for (seqIdx in 0 until seqLen) {
        for (tokenIdx in attentionMask.indices) {
            val v =
                hiddenState.objectAtIndexedSubscript(tokenIdx * seqLen.toLong() + seqIdx).floatValue
            result[seqIdx] += v * attentionMask[tokenIdx]
        }
        result[seqIdx] /= attentionCount
    }

    result
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

