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
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.Language
import com.zhufucdev.practiso.platform.LanguageIdentifier
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import usearch.Index
import usearch.IndexOptions
import usearch.ScalarKind
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.math.abs

sealed class MissingModelResponse {
    data object ProceedAnyway : MissingModelResponse()
    data object Cancel : MissingModelResponse()
}

sealed class DbUpgradeState {
    data class Ready(val index: Index) : DbUpgradeState()
    object Collecting : DbUpgradeState()
    data class MissingModel(
        val current: MlModel?,
        val missingFeatures: Set<ModelFeature>,
        val proceed: SendChannel<MissingModelResponse>? = null,
    ) : DbUpgradeState()

    data class InProgress(val total: Int, val done: Int) : DbUpgradeState()
}

private data class UpgradeProgress(
    val textTotal: Int,
    val textDone: Int,
    val imageTotal: Int,
    val imageDone: Int,
)

private data class FrameUpdate<T>(val addition: Set<T>, val removal: Set<T>, val reduction: Set<T>)

private fun <T> emptyUpdate() = FrameUpdate<T>(emptySet(), emptySet(), emptySet())

class FeiService(private val db: AppDatabase = Database.app, private val parallelTasks: Int = 8) {
    companion object {
        val INDEX_PATH = getPlatform().resourcePath.resolve("search").resolve("embeddings.index")
        const val EMBEDDING_TOP_KEY = "embedding_top"
        const val FEI_MODEL_KEY = "fei_model"
    }

    fun getFeiModel(): Flow<MlModel?> =
        db.settingsQueries.getTextByKey(FEI_MODEL_KEY)
            .asFlow()
            .mapToOneOrDefault(JinaV2SmallEn.hfId, Dispatchers.IO)
            .map(KnownModels::get)

    suspend fun getLanguages(frames: Set<TextFrame>): Set<Language> = coroutineScope {
        val identifier = LanguageIdentifier()
        frames.chunked(frames.size / parallelTasks)
            .map { async { it.map { f -> identifier.getLanguage(f.content) }.toSet() } }
            .awaitAll()
            .flatten()
            .toSet()
    }

    suspend fun getEmbeddings(
        frames: Set<TextFrame>,
        model: MlModel,
    ): List<Pair<TextFrame, FloatArray>> =
        coroutineScope {
            val fei = FrameEmbeddingInference(model)
            frames.chunked(frames.size / parallelTasks)
                .map { frames ->
                    async {
                        fei.getEmbeddings(frames.map { Frame.Text(it.id, it) })
                            .mapIndexed { idx, r -> frames[idx] to r }
                    }
                }
                .awaitAll()
                .flatten()
        }

    private fun getSearchIndex(embeddingFeature: EmbeddingOutput) =
        Index(IndexOptions(embeddingFeature.dimensions, embeddingFeature.metric, ScalarKind.F32))

    private fun Index.maybeLoad(): Boolean {
        val fs = getPlatform().filesystem
        if (!fs.exists(INDEX_PATH.parent!!)) {
            fs.createDirectories(INDEX_PATH.parent!!, true)
        }
        if (fs.exists(INDEX_PATH)) {
            val ba = fs.source(INDEX_PATH).buffer().readByteArray()
            loadBuffer(ba)
            return true
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

            add(LanguageInput(getLanguages(textFrames) - supportedLanguages))
            if (!hasImageFeature && imageFrames.isNotEmpty()) {
                add(ImageInput)
            }
            if (!hasEmbeddingOutput) {
                add(AnyEmbeddingOutput)
            }
        }

    @OptIn(ExperimentalAtomicApi::class)
    private val upgradeStateFlow: Flow<DbUpgradeState> = channelFlow {
        var shouldReadIndexFile = true
        var textFrames: FrameUpdate<TextFrame> = emptyUpdate()
        var imageFrames: FrameUpdate<ImageFrame> = emptyUpdate()
        val updateChannel = Channel<Int>()

        run {
            val updateLock = AtomicInt(0)

            launch {
                db.quizQueries.getAllTextFrames()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { it.toSet() }
                    .collectLatest {
                        val addition = it - textFrames.reduction
                        val removal = textFrames.reduction - it
                        textFrames = FrameUpdate(addition, removal, it)
                        updateChannel.send(abs(updateLock.incrementAndFetch()))
                    }
            }

            launch {
                db.quizQueries.getAllImageFrames()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { it.toSet() }
                    .collectLatest {
                        val addition = it - imageFrames.reduction
                        val removal = imageFrames.reduction - it
                        imageFrames = FrameUpdate(addition, removal, it)
                        updateChannel.send(abs(updateLock.incrementAndFetch()))
                    }
            }
        }

        getFeiModel().collectLatest { model ->
            if (model == null) {
                send(
                    DbUpgradeState.MissingModel(
                        null,
                        withContext(Dispatchers.IO) {
                            getMissingFeatures(
                                null,
                                textFrames.reduction,
                                imageFrames.reduction
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

            send(DbUpgradeState.Collecting)

            var nextEmbeddingKey =
                db.settingsQueries.getIntByKey(EMBEDDING_TOP_KEY).executeAsOneOrNull() ?: 0

            while (coroutineContext.isActive) {
                if (updateChannel.receive() < 2) {
                    continue
                }

                val missingFeatures =
                    getMissingFeatures(model, textFrames.reduction, imageFrames.reduction)
                if (missingFeatures.isNotEmpty()) {
                    val proceedAnyway = Channel<MissingModelResponse>()
                    send(DbUpgradeState.MissingModel(model, missingFeatures, proceedAnyway))
                    when (proceedAnyway.receive()) {
                        MissingModelResponse.Cancel -> return@collectLatest
                        MissingModelResponse.ProceedAnyway -> {}
                    }
                }

                launch(Dispatchers.IO) {
                    if (!model.features.any { it is LanguageInput }) {
                        return@launch
                    }

                    textFrames.removal.forEach { frame -> frame.embeddingsId?.let { index.remove(it.toULong()) } }
                    getEmbeddings(textFrames.addition, model)
                        .forEach { (frame, embedding) ->
                            val key = frame.embeddingsId ?: nextEmbeddingKey++
                            index.asF32.add(key.toULong(), embedding)
                        }

                    db.settingsQueries.setInt(EMBEDDING_TOP_KEY, nextEmbeddingKey)
                    send(DbUpgradeState.Ready(index))
                }
            }
        }
    }

    fun getUpgradeState(): Flow<DbUpgradeState> = upgradeStateFlow
}