package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import io.github.vinceglb.filekit.utils.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.value
import platform.CoreML.MLBatchProviderProtocol
import platform.CoreML.MLFeatureProviderProtocol
import platform.CoreML.MLFeatureValue
import platform.CoreML.MLModelConfiguration
import platform.CoreML.MLMultiArray
import platform.CoreML.MLMultiArrayDataTypeFloat32
import platform.CoreML.MLMultiArrayDataTypeInt32
import platform.CoreML.create
import platform.CoreML.objectAtIndexedSubscript
import platform.CoreML.setObject
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
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
            else -> Language.World
        }
    }
}

typealias MLFeatureResultProducer = (MLFeatureProviderProtocol) -> FloatArray

interface MLFeatureProviderProducer {
    fun one(frame: Frame): Map<String, MLFeatureValue>
    fun many(frames: List<Frame>): Map<String, List<MLFeatureValue>>
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
    override suspend fun getEmbeddings(frames: List<Frame>): List<FloatArray> =
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

    @OptIn(ExperimentalForeignApi::class)
    class FeatureProviderAdapter(val producer: MLFeatureProviderProducer, val frames: List<Frame>) :
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
    val contextWindowSize: Int = 128,
    val tokenizer: Tokenizer,
) : MLFeatureProviderProducer {
    private fun Encoding.getIdMLArray(): MLMultiArray = memScoped {
        val errPtr = allocPointerTo<ObjCObjectVar<NSError?>>()
        val inputIds = MLMultiArray.create(
            shape = listOf(1, contextWindowSize),
            dataType = MLMultiArrayDataTypeFloat32,
            error = errPtr.value
        )
        errPtr.value?.let {
            throw IllegalStateException(it.pointed.value!!.localizedDescription)
        }
        inputIds!!.apply {
            ids.forEachIndexed { idx, it ->
                setObject(NSNumber(it.toInt()), atIndexedSubscript = idx.toLong())
            }
        }
    }

    private fun Encoding.getAttentionMaskMLArray(): MLMultiArray = memScoped {
        val errPtr = allocPointerTo<ObjCObjectVar<NSError?>>()
        val mask = MLMultiArray.create(
            shape = listOf(1, contextWindowSize),
            dataType = MLMultiArrayDataTypeInt32,
            error = errPtr.value
        )
        errPtr.value?.let {
            throw IllegalStateException(it.pointed.value!!.localizedDescription)
        }
        mask!!.apply {
            attentionMask.forEachIndexed { idx, it ->
                setObject(NSNumber(it.toInt()), atIndexedSubscript = idx.toLong())
            }
        }
    }


    override fun one(frame: Frame): Map<String, MLFeatureValue> =
        when (frame) {
            is Frame.Text -> {
                val tokens = tokenizer.encode(frame.textFrame.content)
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

            else -> emptyMap()
        }

    override fun many(frames: List<Frame>): Map<String, List<MLFeatureValue>> =
        buildMap {
            val textEncodings =
                tokenizer.encode(frames.filterIsInstance<Frame.Text>().map { it.textFrame.content })
            val ids =
                textEncodings.map { MLFeatureValue.featureValueWithMultiArray(it.getIdMLArray()) }
            val attentionMasks =
                textEncodings.map { MLFeatureValue.featureValueWithMultiArray(it.getAttentionMaskMLArray()) }
            set("input_ids", ids)
            set("attention_mask", attentionMasks)
        }
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private class SingleOutputResultProducer(val featureName: String) : MLFeatureResultProducer by {
    val value = it.featureValueForName(featureName)!!.multiArrayValue!!
    val shape = value.shape as List<NSNumber>
    assert(shape.size == 2 && (shape.first() as NSNumber).intValue == 1) { "shape error, (1, n) expected, got (${value.shape.joinToString()})" }
    assert(value.dataType == MLMultiArrayDataTypeFloat32) { "type error" }
    FloatArray(shape[1].intValue) { idx -> value.objectAtIndexedSubscript(idx.toLong()).floatValue }
}

actual suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference =
    suspendCoroutine { c ->
        if (model == JinaV2SmallEn) {
            val url =
                NSBundle.mainBundle.URLForResource("JinaV2EnBase", withExtension = "mlmodelc")!!
            CoreMLModel.loadContentsOfURL(url, MLModelConfiguration()) { m, e ->
                if (e != null) {
                    c.resumeWithException(IllegalStateException(e.localizedDescription))
                } else {
                    val url = NSBundle.mainBundle.URLForResource("JinaV2EnTokenizer", "json")!!
                    val ba = NSData.dataWithContentsOfURL(url)!!.toByteArray()
                    val tokenizer = Tokenizer.fromBytes(ba)
                    c.resume(
                        CoreMLInference(
                            model = model,
                            ml = m!!,
                            providerProducer = JinaModelProviderProducer(tokenizer = tokenizer),
                            resultProducer = SingleOutputResultProducer(featureName = "pooler_output")
                        )
                    )
                }
            }
        } else {
            TODO()
        }
    }