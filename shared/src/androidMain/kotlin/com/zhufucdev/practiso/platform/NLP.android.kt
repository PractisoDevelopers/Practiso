package com.zhufucdev.practiso.platform

import android.content.res.AssetFileDescriptor
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.zhufucdev.practiso.HfDirectoryWalker
import com.zhufucdev.practiso.HfSingleFileWalker
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.ListedDirectoryWalker
import com.zhufucdev.practiso.R
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.FeiException
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.moved
import com.zhufucdev.practiso.service.FeiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okio.BufferedSource
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import tokenizers.Encoding
import tokenizers.Tokenizer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

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

actual fun createFrameEmbeddingInference(
    model: MlModel,
    session: InferenceSession,
): Flow<InferenceModelState> = flow {
    val compatibilityList = CompatibilityList()
    val options = Interpreter.Options().apply {
        if (!session.cpuOnly && compatibilityList.isDelegateSupportedOnThisDevice) {
            addDelegate(GpuDelegate(compatibilityList.bestOptionsForThisDevice))
        } else {
            numThreads = getPlatform().logicalProcessorsCount
        }
    }
    when (model) {
        JinaV2SmallEn -> {
            val tokenizer =
                SharedContext.resources.openRawResource(R.raw.jina_v2_en_small_tokenizer)
                    .use { Tokenizer.fromBytes(it.readBytes()) }
            val bf = SharedContext.resources
                .openRawResourceFd(R.raw.jina_v2_en_small)
                .toMappedByteBuffer()
            try {
                emit(
                    InferenceModelState.Complete(
                        LiteRtInference(
                            model = model,
                            inputProducer = BertLiteRtInputProducer(
                                tokenizer,
                                model.sequenceLength ?: 512
                            ),
                            interpreterProducer = {
                                Interpreter(bf, options)
                            },
                            maxParallelInferences = session.maxParallelInferences,
                            maxParallelInfLimit = FeiService.MAX_BATCH_SIZE
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                throw FeiException(
                    scope = AppScope.FeiInitialization,
                    cause = e,
                    appMessage = AppMessage.IncompatibleModel
                )
            }
        }

        else -> {
            val platform = getPlatform()
            val fs = platform.filesystem
            val modelName = model.hfId.replace('/', '-')
            val modelContainer = platform.resourcePath.resolve("LiteRT").resolve(modelName)
            if (!fs.exists(modelContainer)) {
                fs.createDirectories(modelContainer, mustCreate = true)
            }

            val localTfLiteModel = modelContainer.resolve("model.tflite")
            val localTokenizer = modelContainer.resolve("tokenizer.json")
            if (!fs.exists(localTfLiteModel) || !fs.exists(localTokenizer)) {
                val missingFiles = ListedDirectoryWalker(
                    files = buildList {
                        if (!fs.exists(localTfLiteModel)) {
                            val hfRepo =
                                HfDirectoryWalker(model.hfId, path = "LiteRT")
                                    .throwsFeiError()
                                    .moved("LiteRT")
                            val modelFile = hfRepo.files.toList()
                            addAll(modelFile)
                        }
                        if (!fs.exists(localTokenizer)) {
                            val tokenizerFile =
                                HfSingleFileWalker(
                                    model.hfId,
                                    path = "tokenizer.json"
                                ).throwsFeiError().files.first()
                            add(tokenizerFile)
                        }
                    },
                    identifier = model.hfId
                )
                downloadRecursively(missingFiles, modelContainer)
                    .mapToGroup()
                    .collect {
                        when (it) {
                            is GroupedDownloadState.Progress -> {
                                emit(
                                    InferenceModelState.Download(
                                        it.ongoingDownloads,
                                        it.completedFiles,
                                        it.overallProgress
                                    )
                                )
                            }

                            is GroupedDownloadState.Planed -> {
                                emit(
                                    InferenceModelState.PlanDownload(
                                        it.filesToDownload,
                                        it.configure
                                    )
                                )
                            }

                            else -> {}
                        }
                    }
            }

            val tokenizer =
                Tokenizer.fromBytes(fs.read(localTokenizer, BufferedSource::readByteArray))
            val modelFileChannel = Files.newByteChannel(
                localTfLiteModel.toNioPath(),
                StandardOpenOption.READ
            ) as FileChannel
            val modelBuffer =
                modelFileChannel.let { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }

            try {
                emit(
                    InferenceModelState.Complete(
                        LiteRtInference(
                            model = model,
                            inputProducer = BertLiteRtInputProducer(
                                tokenizer,
                                model.sequenceLength ?: 512
                            ),
                            interpreterProducer = {
                                Interpreter(modelBuffer, options)
                            },
                            maxParallelInferences = session.maxParallelInferences,
                            maxParallelInfLimit = FeiService.MAX_BATCH_SIZE
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                modelFileChannel.close()
                throw FeiException(
                    cause = e,
                    scope = AppScope.FeiInitialization,
                    appMessage = AppMessage.IncompatibleModel
                )
            }
        }
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

/**
 * An LiteRT wrapper for dynamic scaling.
 *
 * @param model The loading [MlModel]
 * @param interpreterProducer To produce [InterpreterApi]. This function should guarantee producing the same result everytime
 * @param maxParallelInferences A [Flow] for scaling limit, meaning no more inferences than it will happen at the same time
 * @param maxParallelInfLimit No more [InterpreterApi] will be created than it, and panics if [maxParallelInferences] floats above this value.
 */
class LiteRtInference(
    override val model: MlModel,
    val interpreterProducer: InterpreterProducer,
    val inputProducer: LiteRtInputProducer,
    val maxParallelInferences: StateFlow<Int>,
    val maxParallelInfLimit: Int,
) : FrameEmbeddingInference {
    private val primaryInterpreter = interpreterProducer()
    private val outputTensors =
        (0 until primaryInterpreter.outputTensorCount).associateWith {
            primaryInterpreter.getOutputTensor(
                it
            )
        }

    private val inputIdsTensor = primaryInterpreter.getInputTensor(0) ?: error("No input_ids slot.")
    private val attentionMaskTensor =
        primaryInterpreter.getInputTensor(1) ?: error("No attention_mask slot.")
    private val poolerOutTensor =
        outputTensors.entries.firstOrNull { (idx, t) -> t.shape()[0] == modelBatchSize && t.shape().size == 2 }
            ?: error("No output slot.")

    private val interpreterSemaphore = Semaphore(maxParallelInfLimit)
    private val latestOccupationTime = MutableStateFlow(Duration.INFINITE)

    private val modelBatchSize get() = inputIdsTensor.shape()[0]
    private val modelSeqLen get() = inputIdsTensor.shape()[1]
    private val modelOutputLen get() = poolerOutTensor.value.shape()[1]

    private val interpreterMutex = Mutex()
    private val freeInterpreters = mutableListOf(primaryInterpreter)

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val lifecycleScope = CoroutineScope(newSingleThreadContext("LiteRtInference"))

    init {
        with(lifecycleScope) {
            launch {
                // keep acquirable interpreters under control of maxParallelInferences
                maxParallelInferences.collectLatest {
                    val deprivation = maxParallelInfLimit - it
                    if (deprivation < 0) {
                        error("maxParallelInferences($it) overflowing maxParallelInfLimit($maxParallelInfLimit)")
                    }
                    repeat(deprivation) {
                        interpreterSemaphore.acquire()
                    }
                }
            }

            launch {
                // for evey 120% of latestOccupationTime, close each freeInterpreters
                latestOccupationTime.collectLatest { occupation ->
                    Log.d(
                        "LiteRT",
                        "Latest inference took ${occupation.toDouble(DurationUnit.SECONDS)} seconds"
                    )
                    while (freeInterpreters.isNotEmpty()) {
                        delay(occupation * 1.2)
                        interpreterMutex.withLock {
                            freeInterpreters.forEach(InterpreterApi::close)
                            freeInterpreters.clear()
                            Log.d("LiteRT", "Free interpreters all cleared")
                        }
                    }
                }
            }
        }
    }

    private suspend fun <T> withInterpreter(block: suspend (InterpreterApi) -> T): T {
        Log.d("LiteRT", "${interpreterSemaphore.availablePermits} interpreters available")
        return interpreterSemaphore.withPermit {
            val interpreter = interpreterMutex.withLock {
                freeInterpreters.removeLastOrNull() ?: interpreterProducer()
            }
            try {
                val (result, occupation) = measureTimedValue {
                    block(interpreter)
                }
                latestOccupationTime.emit(occupation)
                result
            } finally {
                freeInterpreters.add(interpreter)
            }
        }
    }

    override suspend fun getEmbeddings(frame: Frame): FloatArray =
        getEmbeddings(listOf(frame)).first()

    override suspend fun <T : Frame> getEmbeddings(frames: List<T>): List<FloatArray> =
        coroutineScope {
            inputProducer.many(frames)
                .chunked(modelBatchSize)
                .map { chunk ->
                    async {
                        withInterpreter { interpreter ->
                            val zeros by lazy {
                                IntArray(modelSeqLen)
                            }
                            val inputs = arrayOf(
                                (chunk.map { (ids, _) -> ids } + List(modelBatchSize - chunk.size) { zeros }).toTypedArray(),
                                (chunk.map { (_, mask) -> mask } + List(modelBatchSize - chunk.size) { zeros }).toTypedArray()
                            )
                            val outputs = buildMap {
                                put(
                                    poolerOutTensor.key,
                                    Array(modelBatchSize) { FloatArray(modelOutputLen) })
                            }

                            interpreter.runForMultipleInputsOutputs(inputs, outputs)
                            outputs.values.first().toList().slice(0 until chunk.size)
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }

    override fun close() {
        freeInterpreters.onEach(InterpreterApi::close)
        lifecycleScope.cancel()
    }
}

typealias InterpreterProducer = () -> InterpreterApi

interface LiteRtInputProducer {
    fun one(frame: Frame): LiteRtInput
    fun <T : Frame> many(frame: List<T>): List<LiteRtInput>
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

class BertLiteRtInputProducer(val tokenizer: Tokenizer, val sequenceLength: Int) :
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

    override fun <T : Frame> many(frames: List<T>): List<LiteRtInput> {
        if (frames.isEmpty()) {
            return emptyList()
        }

        when (frames[0]) {
            is Frame.Text -> {
                val text = frames.map { (it as Frame.Text).textFrame.content }
                return tokenizer.encode(text).map { it.toLiteRtInput() }
            }

            else -> throw UnsupportedOperationException("Embedding inference on ${frames[0]::class.simpleName} is not supported.")
        }
    }
}

actual data class InferenceSession(
    val cpuOnly: Boolean = false,
    val maxParallelInferences: StateFlow<Int> = MutableStateFlow(FeiService.MAX_BATCH_SIZE),
) {
    actual companion object {
        actual val default: InferenceSession = InferenceSession()
    }
}