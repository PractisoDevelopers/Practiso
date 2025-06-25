package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.GetDimensionQuizIds
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.datamodel.AnsweredQuiz
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.SessionCreator
import com.zhufucdev.practiso.datamodel.getAllAnsweredQuizzes
import com.zhufucdev.practiso.datamodel.getQuizByFrame
import com.zhufucdev.practiso.datamodel.toDimensionOptionFlow
import com.zhufucdev.practiso.helper.ratioOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

data class RecommendationServiceConfiguration(
    val searchK: Int = 10,
    val idealSimilarity: Float = 0.8f,
    val idealItemCount: Int = 5,
) {
    companion object {
        val default = RecommendationServiceConfiguration()
    }
}

class RecommendationService(
    private val db: AppDatabase = Database.app,
    private val fei: FeiService = Database.fei,
    private val config: RecommendationServiceConfiguration = RecommendationServiceConfiguration.default,
) {
    fun getRecentRecommendations(): Flow<List<SessionCreator>> =
        db.quizQueries.getRecentQuiz()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .combine(
                db.dimensionQueries.getRecentDimensions(config.idealItemCount.toLong())
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .toDimensionOptionFlow(db.quizQueries),
                ::Pair
            )
            .map { (quizzes, dimensions) ->
                buildList {
                    if (quizzes.isNotEmpty()) {
                        add(
                            SessionCreator.RecentlyCreatedQuizzes(
                                selection = Selection(
                                    quizIds = quizzes.map(Quiz::id).toSet()
                                ),
                                leadingQuizName = quizzes.first().name
                            )
                        )
                    }

                    dimensions.forEach {
                        if (it.quizCount <= 0) {
                            return@forEach
                        }
                        add(
                            SessionCreator.RecentlyCreatedDimension(
                                selection = Selection(
                                    dimensionIds = setOf(it.dimension.id)
                                ),
                                quizCount = it.quizCount,
                                dimensionName = it.dimension.name,
                            )
                        )
                    }
                }
            }

    // TODO: recommend based on error rates, quiz legitimacy, etc
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSmartRecommendations(): Flow<List<SessionCreator>> =
        db.getAllAnsweredQuizzes()
            .combine(fei.getUpgradeState().filterIsInstance<FeiDbState.Ready>(), ::Pair)
            .map { (answeredQuizzes, fei) ->
                val embedding =
                    fei.inference.model.embeddingOutput ?: error("Model missing due features.")

                val quizFailedMuch = coroutineScope {
                    answeredQuizzes.ratioOf(AnsweredQuiz::isCorrect)
                        .entries
                        .filter { it.value < 1 }
                        .sortedByDescending { it.value }
                        .map { (occurrence, _) ->
                            occurrence.quiz.frames
                                .map { option ->
                                    async {
                                        while (true) {
                                            val result = runCatching {
                                                fei.getApproximateNearestNeighbors(
                                                    option.frame,
                                                    config.searchK
                                                )
                                                    .mapNotNull { (key, distance) ->
                                                        key.takeIf {
                                                            embedding.normalizer(
                                                                distance
                                                            ) >= config.idealSimilarity
                                                        }
                                                    }
                                            }
                                            when (val e = result.exceptionOrNull()) {
                                                null -> return@async result.getOrThrow()
                                                is FrameIndexNotSupportedException -> {
                                                    // ignore unsupported frames
                                                    return@async emptyList()
                                                }

                                                is FrameNotIndexedException -> {
                                                    e.printStackTrace()
                                                    // TODO: replace this line, ignore for now
                                                    return@async emptyList()
                                                }

                                                is StrandedKeyException -> {
                                                    e.printStackTrace()
                                                    e.keys.forEach { key ->
                                                        fei.index.remove(key)
                                                    }
                                                }
                                            }
                                        }
                                        emptyList() // this is unreachable, it's here because Kotlin type system is ass
                                    }
                                }
                                .awaitAll()
                                .flatten()
                                .toSet()
                                .map { frame ->
                                    async(Dispatchers.IO) {
                                        db.quizQueries.getQuizByFrame(frame).executeAsOne()
                                    }
                                }
                                .awaitAll()
                                .plus(occurrence.quiz.quiz)
                        }
                        .flatten()
                        .toSet()
                }
                if (quizFailedMuch.isEmpty()) {
                    return@map emptyList()
                }

                val quizFailedMuchIds = quizFailedMuch.map(Quiz::id).toSet()

                val dimensionQuizIds =
                    db.dimensionQueries.getDimensionQuizIds()
                        .executeAsList()
                        .groupBy { (id, name) -> Dimension(id, name) }

                buildList {
                    dimensionQuizIds.forEach { (dimension, value) ->
                        val selection = Selection(
                            quizIds = value.filter { it.quizId in quizFailedMuchIds }
                                .map(GetDimensionQuizIds::quizId)
                                .toSet()
                        )
                        if (selection.quizIds.isNotEmpty()) {
                            add(
                                SessionCreator.FailMuchDimension(
                                    dimension = dimension,
                                    selection = selection,
                                    itemCount = selection.quizIds.size
                                )
                            )
                        }
                    }
                    add(
                        SessionCreator.FailMuch(
                            selection = Selection(quizFailedMuchIds),
                            leadingItemName = quizFailedMuch.firstNotNullOfOrNull { it.name },
                            itemCount = quizFailedMuch.size
                        )
                    )
                }
            }
}