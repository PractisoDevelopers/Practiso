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
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.ErrorMessage
import com.zhufucdev.practiso.datamodel.ErrorModel
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.ImageInput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.datamodel.ModelFeature
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.helper.readFrom
import com.zhufucdev.practiso.helper.saveTo
import com.zhufucdev.practiso.platform.DownloadDiscretion
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.InferenceModelState
import com.zhufucdev.practiso.platform.InferenceState
import com.zhufucdev.practiso.platform.Language
import com.zhufucdev.practiso.platform.LanguageIdentifier
import com.zhufucdev.practiso.platform.createFrameEmbeddingInference
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.platform.lastCompletion
import com.zhufucdev.practiso.platform.platformGetEmbeddings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.use
import usearch.Index
import usearch.IndexOptions
import usearch.ScalarKind
import kotlin.time.Duration.Companion.seconds

class FeiService(
    private val db: AppDatabase = Database.app,
    private val defaultModel: MlModel = JinaV2SmallEn,
    private val parallelTasks: Int = 8,
) :
    CoroutineScope by CoroutineScope(Dispatchers.Main) {
    companion object {
        val INDEX_PATH = getPlatform().resourcePath.resolve("search").resolve("embeddings.index")
        const val EMBEDDING_TOP_KEY = "embedding_top"
        const val DB_FEI_VERSION_KEY = "db_fei_version"
        const val INDEX_FEI_VERSION_KEY = "index_fei_version"
        const val FEI_MODEL_KEY = "fei_model" // Frame Embedding Inference
        const val DB_FEI_MODEL_KEY = "db_fei_model"
        const val MAX_BATCH_SIZE = 16
        val DEBOUNCE_TIMEOUT = 1.seconds
    }

    fun getFeiModel(): Flow<MlModel?> =
        db.settingsQueries.getTextByKey(FEI_MODEL_KEY)
            .asFlow()
            .mapToOneOrDefault(defaultModel.hfId, Dispatchers.IO)
            .distinctUntilChanged()
            .map(KnownModels::get)

    @Throws(CancellationException::class, IllegalArgumentException::class)
    suspend fun setFeiModel(model: MlModel) = db.transaction {
        db.settingsQueries.setText(
            FEI_MODEL_KEY,
            KnownModels[model.hfId]?.hfId
                ?: throw IllegalArgumentException("Unknown model: ${model.hfId}")
        )
    }

    @Throws(CancellationException::class, IllegalArgumentException::class)
    fun setFeiModelSync(model: MlModel) = runBlocking { setFeiModel(model) }

    suspend fun getLanguages(frames: Set<TextFrame>): Set<Language> = coroutineScope {
        val identifier = LanguageIdentifier()
        frames.chunked(frames.size / parallelTasks + 1)
            .map { async { it.map { f -> identifier.getLanguage(f.content) } } }
            .awaitAll()
            .flatten()
            .toSet()
    }

    private fun getSearchIndex(embeddingFeature: EmbeddingOutput) =
        Index(
            IndexOptions(
                embeddingFeature.dimensions,
                embeddingFeature.metric,
                ScalarKind.F32 // TODO: fix usearch binding in f16 quantization
            )
        )

    private fun Index.maybeLoad(): Boolean {
        val fs = getPlatform().filesystem
        ensureSearchDirectory(fs)
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
            val supportedLanguages = model?.inputLanguages ?: emptySet()
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

    @OptIn(FlowPreview::class)
    private val upgradeStateFlow: SharedFlow<FeiDbState> = channelFlow {
        var shouldReadIndexFile = true
        val textFrameFlow =
            db.quizQueries.getAllTextFrames()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .debounce(DEBOUNCE_TIMEOUT)
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
                .debounce(DEBOUNCE_TIMEOUT)
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

        getFeiModel().combine(getPlatform().getInferenceSession(), ::Pair)
            .collectLatest { (model, session) ->
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

                val dbFeiVersion =
                    db.settingsQueries.getIntByKey(DB_FEI_VERSION_KEY).executeAsOneOrNull()
                val indexFeiVersion =
                    db.settingsQueries.getIntByKey(INDEX_FEI_VERSION_KEY).executeAsOneOrNull()

                db.transaction {
                    if (dbFeiVersion == null) {
                        db.settingsQueries.setInt(DB_FEI_VERSION_KEY, 0)
                    }
                    if (indexFeiVersion == null) {
                        db.settingsQueries.setInt(INDEX_FEI_VERSION_KEY, 0)
                    }
                }

                val (index, indexInvalid) = suspend {
                    val dbModelId =
                        db.settingsQueries.getTextByKey(DB_FEI_MODEL_KEY).executeAsOneOrNull()
                            ?: defaultModel.hfId
                    val embeddingFeature =
                        model.features.filterFirstIsInstanceOrNull<EmbeddingOutput>()!!

                    if (dbModelId == model.hfId && shouldReadIndexFile && dbFeiVersion != null && dbFeiVersion == indexFeiVersion) {
                        withContext(Dispatchers.IO) {
                            getSearchIndex(embeddingFeature).let {
                                shouldReadIndexFile = false
                                it to !it.maybeLoad()
                            }
                        }
                    } else {
                        getSearchIndex(embeddingFeature) to true
                    }
                }()

                val feiStateFlow = createFrameEmbeddingInference(model, session)
                    .flowOn(Dispatchers.IO)
                    .onEach { download ->
                        when (download) {
                            is InferenceModelState.Download -> {
                                send(
                                    FeiDbState.DownloadingInference(
                                        progress = download.overallProgress,
                                        model = model
                                    )
                                )
                                true
                            }

                            is InferenceModelState.PlanDownload -> {
                                val responseChannel = Channel<PendingDownloadResponse>()
                                send(
                                    FeiDbState.PendingDownload(
                                        download.files,
                                        responseChannel
                                    )
                                )
                                when (responseChannel.receive()) {
                                    PendingDownloadResponse.Discretion -> {
                                        download.build {
                                            discretion = DownloadDiscretion.Discretionary
                                        }
                                    }

                                    PendingDownloadResponse.Immediate -> {
                                        download.build {
                                            discretion = DownloadDiscretion.Immediate
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }

                var fei: FrameEmbeddingInference
                while (true) {
                    try {
                        fei = feiStateFlow.lastCompletion()
                        break
                    } catch (e: FeiInitializationException) {
                        val responseChannel = Channel<FeiErrorResponse>()
                        send(
                            FeiDbState.Error(
                                ErrorModel(
                                    scope = AppScope.FeiInitialization,
                                    error = e.cause,
                                    message = e.errorMsg
                                ), responseChannel
                            )
                        )
                        when (responseChannel.receive()) {
                            FeiErrorResponse.Retry -> continue
                        }
                    }
                }

                try {
                    send(FeiDbState.Ready(index, db, model))

                    val initialDrops = if (indexInvalid) 0 else 1
                    textFrameFlow.drop(initialDrops).addPacing()
                        .combine(imageFrameFlow.drop(initialDrops).addPacing(), ::Pair)
                        .filterNot { (a, b) -> a.isEmpty() && b.isEmpty() }
                        .buffer()
                        .collect { (textFrames, imageFrames) ->
                            send(FeiDbState.Collecting)

                            withContext(Dispatchers.IO) {
                                db.transaction {
                                    db.settingsQueries.bumpInt(DB_FEI_VERSION_KEY)
                                }
                            }

                            val missingFeatures =
                                withContext(Dispatchers.IO) {
                                    getMissingFeatures(
                                        model,
                                        textFrames.addition,
                                        imageFrames.addition
                                    )
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

                            val totalFramesCount =
                                textFrames.addition.size + textFrames.removal.size
                            var completedFramesCount = 0

                            send(FeiDbState.InProgress(totalFramesCount, completedFramesCount))

                            try {
                                db.embeddingQueries.getIndexKeyByFrameIds(
                                    textFrameId = textFrames.removal.map(TextFrame::id),
                                    imageFrameId = imageFrames.removal.map(ImageFrame::id)
                                )
                                    .executeAsList()
                                    .forEach { index.remove(it.toULong()) }

                                db.transaction {
                                    db.embeddingQueries.removeIndexByFrameIds(
                                        textFrameId = textFrames.removal.map(TextFrame::id),
                                        imageFrameId = imageFrames.removal.map(ImageFrame::id)
                                    )
                                }

                                platformGetEmbeddings(
                                    textFrames.addition,
                                    imageFrames.addition,
                                    fei,
                                    parallelTasks
                                ).flowOn(Dispatchers.IO).collect { state ->
                                    when (state) {
                                        is InferenceState.Complete -> {
                                            db.transaction {
                                                state.results.forEach { (row, ebd) ->
                                                    val dbKey = db.embeddingQueries
                                                        .getIndexKeyByFrameId(
                                                            row.textFrameId,
                                                            row.imageFrameId
                                                        )
                                                        .executeAsOneOrNull()
                                                    val key = dbKey
                                                        ?: nextEmbeddingKeyMutex.withLock { nextEmbeddingKey++ }

                                                    index.asF32.add(key.toULong(), ebd)
                                                    if (dbKey == null) {
                                                        db.embeddingQueries.insertIndex(
                                                            row.textFrameId,
                                                            row.imageFrameId,
                                                            key
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        is InferenceState.Inferring -> {
                                            completedFramesCount += state.done
                                            send(
                                                FeiDbState.InProgress(
                                                    total = totalFramesCount,
                                                    done = completedFramesCount
                                                )
                                            )
                                        }
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
                                val fs = getPlatform().filesystem
                                ensureSearchDirectory(fs)
                                fs.sink(INDEX_PATH).use {
                                    index.saveTo(it)
                                }
                                db.transaction {
                                    db.settingsQueries.copyInt(
                                        DB_FEI_VERSION_KEY,
                                        INDEX_FEI_VERSION_KEY
                                    )
                                    db.settingsQueries.setText(DB_FEI_MODEL_KEY, model.hfId)
                                }
                            }
                        }
                } finally {
                    fei.close()
                }
            }
    }.shareIn(
        this, started = SharingStarted.Eagerly, replay = 1
    )

    fun getUpgradeState(): Flow<FeiDbState> = upgradeStateFlow

    private fun ensureSearchDirectory(fs: FileSystem = getPlatform().filesystem) {
        val dir = INDEX_PATH.parent!!
        if (!fs.exists(dir))
            fs.createDirectories(dir, mustCreate = true)
    }
}

sealed class MissingModelResponse {
    data object ProceedAnyway : MissingModelResponse()
    data object Cancel : MissingModelResponse()
}

sealed class PendingDownloadResponse {
    data object Immediate : PendingDownloadResponse()
    data object Discretion : PendingDownloadResponse()
}

sealed class FeiErrorResponse {
    data object Retry : FeiErrorResponse()
}

sealed class FeiDbState {
    data class Ready(val index: Index, val db: AppDatabase, val model: MlModel) : FeiDbState()
    object Collecting : FeiDbState()
    data class MissingModel(
        val current: MlModel?,
        val missingFeatures: Set<ModelFeature>,
        val proceed: SendChannel<MissingModelResponse>? = null,
    ) : FeiDbState()

    data class PendingDownload(
        val files: List<DownloadableFile>,
        val response: SendChannel<PendingDownloadResponse>,
    ) : FeiDbState()

    data class Error(
        val error: ErrorModel,
        val proceed: SendChannel<FeiErrorResponse>,
    ) : FeiDbState()

    data class DownloadingInference(val progress: Float, val model: MlModel) : FeiDbState()

    data class InProgress(val total: Int, val done: Int) : FeiDbState()
}

class FeiInitializationException(val errorMsg: ErrorMessage, override val cause: Throwable) :
    Exception(cause)

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
        is Frame.Image -> db.embeddingQueries.getImageIndex(frame.id).executeAsOneOrNull()
        is Frame.Text -> db.embeddingQueries.getTextIndex(frame.id).executeAsOneOrNull()
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
                    val fei = db.embeddingQueries.getIndexByKey(key.toLong())
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
