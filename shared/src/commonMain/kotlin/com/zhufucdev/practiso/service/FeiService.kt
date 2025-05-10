package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrDefault
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.KnownModels
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.AnyEmbeddingOutput
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.ImageInput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.datamodel.ModelFeature
import com.zhufucdev.practiso.helper.readFrom
import com.zhufucdev.practiso.helper.saveTo
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.Language
import com.zhufucdev.practiso.platform.LanguageIdentifier
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import usearch.Index
import usearch.IndexOptions

class FeiService(private val db: AppDatabase = Database.app, private val parallelTasks: Int = 8) :
    CoroutineScope by CoroutineScope(Dispatchers.Main) {
    companion object {
        val INDEX_PATH = getPlatform().resourcePath.resolve("search").resolve("embeddings.index")
        const val EMBEDDING_TOP_KEY = "embedding_top"
        const val FEI_MODEL_KEY = "fei_model" // Frame Embedding Inference
        const val MAX_BATCH_SIZE = 128
    }

    fun getFeiModel(): Flow<MlModel?> =
        db.settingsQueries.getTextByKey(FEI_MODEL_KEY)
            .asFlow()
            .mapToOneOrDefault(JinaV2SmallEn.hfId, Dispatchers.IO)
            .map(KnownModels::get)

    suspend fun getLanguages(frames: Set<TextFrame>): Set<Language> = coroutineScope {
        val identifier = LanguageIdentifier()
        frames.chunked(frames.size / parallelTasks + 1)
            .map { async { it.map { f -> identifier.getLanguage(f.content) } } }
            .awaitAll()
            .flatten()
            .toSet()
    }

    fun getEmbeddings(
        frames: Set<TextFrame>,
        model: MlModel,
    ): Flow<InferenceState> = channelFlow {
        val fei = FrameEmbeddingInference(model)
        val total = frames.size
        val batchSize = minOf(frames.size / parallelTasks + 1, MAX_BATCH_SIZE)
        coroutineScope {
            val jobs = frames.chunked(batchSize)
                .map { textFrames ->
                    async {
                        val frames = textFrames.map { Frame.Text(it.id, it) }
                        fei.getEmbeddings(frames)
                            .mapIndexed { idx, r -> textFrames[idx] to r }
                            .also {
                                send(InferenceState.Inferring(frames.size, total))
                            }
                    }
                }

            val result = jobs.awaitAll().flatten()
            send(InferenceState.Complete(result))
        }
    }

    private fun getSearchIndex(embeddingFeature: EmbeddingOutput) =
        Index(
            IndexOptions(
                embeddingFeature.dimensions,
                embeddingFeature.metric,
                embeddingFeature.precision
            )
        )

    private fun Index.maybeLoad(): Boolean {
        val fs = getPlatform().filesystem
        if (!fs.exists(INDEX_PATH.parent!!)) {
            fs.createDirectories(INDEX_PATH.parent!!, true)
        }
        if (fs.exists(INDEX_PATH)) {
            try {
                readFrom(fs.source(INDEX_PATH))
                return true
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return false
    }

    private suspend fun getMissingFeatures(
        model: MlModel?,
        textFrames: Set<TextFrame>,
        imageFrames: Set<ImageFrame>,
    ): Set<ModelFeature> =
        buildSet {
            val supportedLanguages = model?.features?.filterIsInstance<LanguageInput>()
                ?.fold(mutableSetOf<Language>()) { acc, curr ->
                    acc.addAll(curr.supports)
                    acc
                }
                ?: emptySet()
            val hasImageFeature = model?.features?.any { it is ImageInput } ?: false
            val hasEmbeddingOutput = model?.features?.any { it is EmbeddingOutput } ?: false

            (getLanguages(textFrames) - supportedLanguages)
                .takeIf { it.isNotEmpty() }
                ?.let { add(LanguageInput(it)) }
            if (!hasImageFeature && imageFrames.isNotEmpty()) {
                add(ImageInput)
            }
            if (!hasEmbeddingOutput) {
                add(AnyEmbeddingOutput)
            }
        }

    private val upgradeStateFlow: SharedFlow<FeiDbState> = channelFlow {
        var shouldReadIndexFile = true
        val textFrameFlow =
            db.quizQueries.getAllTextFrames()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { it.toSet() }
                .runningFold(emptyUpdate<TextFrame>()) { last, current ->
                    val addition = current - last.complete
                    val removal = last.complete - current
                    return@runningFold FrameUpdate(addition, removal, current)
                }
                .drop(1)

        val imageFrameFlow =
            db.quizQueries.getAllImageFrames()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { it.toSet() }
                .runningFold(emptyUpdate<ImageFrame>()) { last, current ->
                    val addition = current - last.complete
                    val removal = last.complete - current
                    return@runningFold FrameUpdate(addition, removal, current)
                }
                .drop(1)

        val nextEmbeddingKeyMutex = Mutex()
        var nextEmbeddingKey =
            db.settingsQueries.getIntByKey(EMBEDDING_TOP_KEY).executeAsOneOrNull() ?: 0

        getFeiModel().collectLatest { model ->
            if (model == null) {
                send(
                    FeiDbState.MissingModel(
                        null,
                        withContext(Dispatchers.IO) {
                            getMissingFeatures(
                                null,
                                textFrameFlow.first().complete,
                                imageFrameFlow.first().complete
                            )
                        }
                    )
                )
                return@collectLatest
            }

            val index = withContext(Dispatchers.IO) {
                getSearchIndex(model.features.first { it is EmbeddingOutput } as EmbeddingOutput).apply {
                    if (shouldReadIndexFile) {
                        maybeLoad()
                        shouldReadIndexFile = false
                    }
                }
            }

            send(FeiDbState.Ready(index, db, model))

            textFrameFlow.drop(1).addPacing().combine(imageFrameFlow.drop(1).addPacing(), ::Pair)
                .filterNot { (a, b) -> a.isEmpty() && b.isEmpty() }
                .collect { (textFrames, imageFrames) ->
                    send(FeiDbState.Collecting)

                    val missingFeatures =
                        withContext(Dispatchers.Default) {
                            getMissingFeatures(model, textFrames.addition, imageFrames.addition)
                        }
                    if (missingFeatures.isNotEmpty()) {
                        val proceedAnyway = Channel<MissingModelResponse>()
                        send(FeiDbState.MissingModel(model, missingFeatures, proceedAnyway))
                        when (proceedAnyway.receive()) {
                            MissingModelResponse.Cancel -> {
                                send(FeiDbState.Ready(index, db, model))
                                return@collect
                            }

                            MissingModelResponse.ProceedAnyway -> {}
                        }
                    }

                    val totalFramesCount = textFrames.addition.size + textFrames.removal.size
                    var completedFramesCount = 0

                    send(FeiDbState.InProgress(totalFramesCount, completedFramesCount))

                    try {
                        coroutineScope {
                            launch(Dispatchers.Default) {
                                if (!model.features.any { it is LanguageInput }) {
                                    return@launch
                                }

                                textFrames.removal
                                    .mapNotNull { frame ->
                                        db.quizQueries.getTextFrameEmbeddingIndex(frame.id)
                                            .executeAsOneOrNull()
                                            ?.let {
                                                index.remove(it.toULong())
                                                async(Dispatchers.IO) {
                                                    db.transaction {
                                                        db.quizQueries.deleteFrameEmbeddingIndexByKey(
                                                            it
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                    .awaitAll()
                                getEmbeddings(textFrames.addition, model).collect {
                                    when (it) {
                                        is InferenceState.Complete -> {
                                            it.results<TextFrame>().forEach { (frame, ebd) ->
                                                db.transaction {
                                                    val dbKey = db.quizQueries
                                                        .getTextFrameEmbeddingIndex(frame.id)
                                                        .executeAsOneOrNull()

                                                    val key = dbKey
                                                        ?: nextEmbeddingKeyMutex.withLock { nextEmbeddingKey++ }
                                                    index.asF32.add(
                                                        key.toULong(),
                                                        ebd
                                                    )
                                                    if (dbKey == null) {
                                                        db.quizQueries.insertTextFrameEmbeddingIndex(
                                                            frame.id,
                                                            key
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        is InferenceState.Inferring -> {
                                            completedFramesCount += it.done
                                            send(
                                                FeiDbState.InProgress(
                                                    total = totalFramesCount,
                                                    done = completedFramesCount
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            launch(Dispatchers.Default) {
                                // TODO: image inference
                            }
                        }
                    } finally {
                        // to avoid poisoning the database
                        db.settingsQueries.setInt(
                            EMBEDDING_TOP_KEY,
                            nextEmbeddingKey
                        )
                    }

                    send(FeiDbState.Ready(index, db, model))

                    withContext(Dispatchers.IO) {
                        index.saveTo(getPlatform().filesystem.sink(INDEX_PATH))
                    }
                }
        }
    }.shareIn(
        this, started = SharingStarted.Eagerly, replay = 1
    )

    fun getUpgradeState(): Flow<FeiDbState> = upgradeStateFlow
}

sealed class MissingModelResponse {
    data object ProceedAnyway : MissingModelResponse()
    data object Cancel : MissingModelResponse()
}

sealed class FeiDbState {
    data class Ready(val index: Index, val db: AppDatabase, val model: MlModel) : FeiDbState()
    object Collecting : FeiDbState()
    data class MissingModel(
        val current: MlModel?,
        val missingFeatures: Set<ModelFeature>,
        val proceed: SendChannel<MissingModelResponse>? = null,
    ) : FeiDbState()

    data class InProgress(val total: Int, val done: Int) : FeiDbState()
}

private data class FrameUpdate<T>(
    val addition: Set<T>,
    val removal: Set<T>,
    val complete: Set<T>,
) {
    fun isEmpty() = addition.isEmpty() && removal.isEmpty()
}

private fun <T> emptyUpdate() = FrameUpdate<T>(emptySet(), emptySet(), emptySet())

/**
 * Add [emptyUpdate] before each emission
 */
private fun <T> Flow<FrameUpdate<T>>.addPacing() =
    flow {
        emit(emptyUpdate<T>())
        this@addPacing.collect {
            emit(it)
            emit(emptyUpdate())
        }
    }


sealed class InferenceState {
    data class Complete(val results: List<Pair<Any, FloatArray>>) : InferenceState() {
        fun <C> results() = results as List<Pair<C, FloatArray>>
    }

    data class Inferring(val done: Int, val total: Int) : InferenceState()
}

class FrameNotIndexedException(frame: Frame) :
    IllegalStateException("${frame::class.simpleName}[id=${frame.id}]")

class FrameIndexNotSupportedException : UnsupportedOperationException()

@Throws(
    FrameIndexNotSupportedException::class, FrameNotIndexedException::class,
    CancellationException::class
)
suspend fun FeiDbState.Ready.getApproximateNearestNeighbors(
    frame: Frame,
    count: Int,
): List<Pair<Frame, Float>> {
    val key = when (frame) {
        is Frame.Image -> db.quizQueries.getImageFrameEmbeddingIndex(frame.id).executeAsOneOrNull()
        is Frame.Text -> db.quizQueries.getTextFrameEmbeddingIndex(frame.id).executeAsOneOrNull()
        else -> throw FrameIndexNotSupportedException()
    }
    if (key == null) {
        throw FrameNotIndexedException(frame)
    }

    val embeddings = index.asF32[key.toULong()] ?: throw FrameNotIndexedException(frame)
    val matches =
        index.search(embeddings, count)

    return coroutineScope {
        matches
            .map { (key, distance) ->
                async(Dispatchers.IO) {
                    val fei = db.quizQueries.getFrameEmbeddingIndexByKey(key.toLong())
                        .executeAsOneOrNull() ?: error("Index key has no associated frame.")
                    val frame = if (fei.textFrameId != null) {
                        db.quizQueries.getTextFrameById(fei.textFrameId).executeAsOne()
                            .let { Frame.Text(id = it.id, textFrame = it) }
                    } else if (fei.imageFrameId != null) {
                        db.quizQueries.getImageFrameById(fei.imageFrameId).executeAsOne()
                            .let { Frame.Image(id = it.id, imageFrame = it) }
                    } else {
                        error("This database is so broken.")
                    }
                    frame to distance
                }
            }
            .awaitAll()
    }
}
