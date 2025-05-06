package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.SessionCreator
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toDimensionOptionFlow
import com.zhufucdev.practiso.datamodel.toQuizOptionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

class RecommendationService(private val db: AppDatabase = Database.app) {
    fun getRecentRecommendations(count: Long = 5): Flow<List<SessionCreator>> =
        channelFlow {
            db.quizQueries.getQuizFrames(db.quizQueries.getRecentQuiz())
                .toQuizOptionFlow()
                .collectLatest { quizzes ->
                    db.dimensionQueries.getRecentDimensions(count)
                        .asFlow()
                        .mapToList(Dispatchers.IO)
                        .toDimensionOptionFlow(db.quizQueries)
                        .collectLatest { dimensions ->
                            val emission = buildList {
                                if (quizzes.isNotEmpty()) {
                                    add(
                                        SessionCreator.RecentlyCreatedQuizzes(
                                            selection = Selection(
                                                quizIds = quizzes.map(PractisoOption::id).toSet()
                                            ),
                                            leadingQuizName = quizzes.first().quiz.name
                                        )
                                    )
                                }

                                dimensions.forEach {
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
                            send(emission)
                        }
                }
        }

    // TODO: recommend based on error rates, quiz legitimacy, etc
    fun getSmartRecommendations(): Flow<List<SessionCreator>> = getRecentRecommendations()
}