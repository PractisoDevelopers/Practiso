package com.zhufucdev.practiso.platform

import android.content.res.AssetFileDescriptor
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.PractisoApp
import com.zhufucdev.practiso.R
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import tokenizers.Encoding
import tokenizers.Tokenizer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Language(bcp47Code: String): Language {
    if (bcp47Code.startsWith("en")) {
        return Language.English
    }
    if (bcp47Code.startsWith("zh")) {
        return Language.Chinese
    }
    if (bcp47Code.startsWith("de")) {
        return Language.German
    }
    if (bcp47Code.startsWith("es")) {
        return Language.Spanish
    }
    return Language.World
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
        numThreads = getPlatform().logicalProcessorsCount
//        if (compatibilityList.isDelegateSupportedOnThisDevice) {
//            addDelegate(GpuDelegate(compatibilityList.bestOptionsForThisDevice))
//        } else {
//        }
    }
    return when (model) {
        is JinaV2SmallEn -> {
            val tokenizer =
                PractisoApp.instance.resources.openRawResource(R.raw.jina_v2_en_small_tokenizer)
                    .use { Tokenizer.fromBytes(it.readBytes()) }
            val interpreter =
                Interpreter(
                    PractisoApp.instance.resources
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
    val inputIdsTensor = interpreter.getInputTensor(0) ?: error("No input_ids slot.")
    val attentionMaskTensor = interpreter.getInputTensor(1) ?: error("No attention_mask slot.")

    override suspend fun getEmbeddings(frame: Frame): FloatArray {
        val (inputIds, attentionMask) = inputProducer.one(frame)
        val input = arrayOf(inputIds, attentionMask)
        val output = mutableMapOf<String, FloatArray>()
        interpreter.run(input, output)
        return output.values.first()
    }

    override suspend fun getEmbeddings(frames: List<Frame>): List<FloatArray> {
        val inputs = inputProducer.many(frames).let {
            arrayOf(
                it.map { (inputIds, _) -> inputIds }.toTypedArray(),
                it.map { (_, attentionMask) -> attentionMask }.toTypedArray()
            )
        }
        val outputs = mutableMapOf<Int, Any>()
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
        return outputs.entries
            .sortedBy { it.key }
            .map { (_, value) -> (value as Map<String, FloatArray>).values.first() }
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

    companion object {
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
