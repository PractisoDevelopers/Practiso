package com.zhufucdev.practiso.platform

import android.content.res.AssetFileDescriptor
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.R
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import tokenizers.Encoding
import tokenizers.Tokenizer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Language(bcp47Code: String): Language =
    when (bcp47Code) {
        "en" -> Language.English
        "zh" -> Language.Chinese
        "de" -> Language.German
        "es" -> Language.Spanish
        "und", "km" -> Language.Default
        else -> Language.World
    }

actual class LanguageIdentifier {
    actual suspend fun getLanguage(text: String): Language = suspendCancellableCoroutine { c ->
        LanguageIdentification.getClient()
            .identifyLanguage(text)
            .addOnSuccessListener {
                c.resume(Language(it))
            }
            .addOnFailureListener {
                c.resumeWithException(it)
            }
            .addOnCanceledListener {
                c.cancel()
            }
    }
}

actual suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference {
    val compatibilityList = CompatibilityList()
    val options = Interpreter.Options().apply {
        if (compatibilityList.isDelegateSupportedOnThisDevice) {
            addDelegate(GpuDelegate(compatibilityList.bestOptionsForThisDevice))
        } else {
            numThreads = getPlatform().logicalProcessorsCount
        }
    }
    return when (model) {
        is JinaV2SmallEn -> {
            val tokenizer =
                SharedContext.resources.openRawResource(R.raw.jina_v2_en_small_tokenizer)
                    .use { Tokenizer.fromBytes(it.readBytes()) }
            val interpreter =
                Interpreter(
                    SharedContext.resources
                        .openRawResourceFd(R.raw.jina_v2_en_small)
                        .toMappedByteBuffer(),
                    options
                )

            LiteRtInference(
                model = model,
                inputProducer = JinaLiteRtInputProducer(tokenizer, 512),
                interpreter = interpreter
            )
        }

        else -> TODO()
    }
}

private fun AssetFileDescriptor.toMappedByteBuffer(): MappedByteBuffer {
    try {
        val inputStream = FileInputStream(fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    } finally {
        close()
    }
}

class LiteRtInference(
    override val model: MlModel,
    val interpreter: InterpreterApi,
    val inputProducer: LiteRtInputProducer,
) : FrameEmbeddingInference {
    private val outputTensors =
        (0 until interpreter.outputTensorCount).associateWith { interpreter.getOutputTensor(it) }
    private val poolerOutTensor =
        outputTensors.entries.firstOrNull { (idx, t) -> t.shape()[0] == 1 && t.shape().size == 2 }
            ?: error("No output slot.")

    private val mutex = Mutex()

    init {
        interpreter.getInputTensor(0) ?: error("No input_ids slot.")
        interpreter.getInputTensor(1) ?: error("No attention_mask slot.")
    }

    private fun getEmbeddings(input: LiteRtInput): FloatArray {
        val input = arrayOf(input.inputIds, input.attentionMask)
        val output = buildMap {
            put(poolerOutTensor.key, arrayOf(FloatArray(poolerOutTensor.value.shape()[1])))
        }
        interpreter.runForMultipleInputsOutputs(input, output)
        return output.values.first().first()
    }

    override suspend fun getEmbeddings(frame: Frame): FloatArray = mutex.withLock {
        val rtInput = inputProducer.one(frame)
        return getEmbeddings(rtInput)
    }

    override suspend fun getEmbeddings(frames: List<Frame>): List<FloatArray> = mutex.withLock {
        inputProducer.many(frames)
            .map(::getEmbeddings)
    }
}

interface LiteRtInputProducer {
    fun one(frame: Frame): LiteRtInput
    fun many(frame: List<Frame>): List<LiteRtInput>
}

data class LiteRtInput(val inputIds: IntArray, val attentionMask: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteRtInput

        if (!inputIds.contentEquals(other.inputIds)) return false
        if (!attentionMask.contentEquals(other.attentionMask)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputIds.contentHashCode()
        result = 31 * result + attentionMask.contentHashCode()
        return result
    }
}

class JinaLiteRtInputProducer(val tokenizer: Tokenizer, val sequenceLength: Int) :
    LiteRtInputProducer {
    fun Encoding.toLiteRtInput() = LiteRtInput(
        IntArray(sequenceLength) { idx ->
            if (idx < size) {
                ids[idx].toInt()
            } else {
                0
            }
        },
        IntArray(sequenceLength) { idx ->
            if (idx < size) {
                attentionMask[idx].toInt()
            } else {
                0
            }
        }
    )

    override fun one(frame: Frame): LiteRtInput =
        when (frame) {
            is Frame.Text -> {
                tokenizer.encode(frame.textFrame.content, true).toLiteRtInput()
            }

            is Frame.Options -> throw UnsupportedOperationException("Embedding inference is not supported on nested frames.")
            else -> throw UnsupportedOperationException("Embedding inference on ${frame::class.simpleName} is not supported.")
        }

    override fun many(frame: List<Frame>): List<LiteRtInput> {
        val text = frame.filterIsInstance<Frame.Text>().map { it.textFrame.content }
        return tokenizer.encode(text).map { it.toLiteRtInput() }
    }
}
