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
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.n_questions_in_dimension
import resources.new_question_para
import resources.x_and_n_more_para

class RecommendationService(private val db: AppDatabase = Database.app) {
    fun getRecentRecommendations(): Flow<List<SessionCreator>> =
        channelFlow {
            db.quizQueries.getQuizFrames(db.quizQueries.getRecentQuiz())
                .toQuizOptionFlow()
                .collectLatest { quizzes ->
                    db.dimensionQueries.getRecentDimensions(5)
                        .asFlow()
                        .mapToList(Dispatchers.IO)
                        .toDimensionOptionFlow(db.quizQueries)
                        .collectLatest { dimensions ->
                            val emission = buildList {
                                if (quizzes.isNotEmpty()) {
                                    val firstName = quizzes.first().quiz.name
                                        ?: getString(Res.string.new_question_para)
                                    add(
                                        SessionCreator.ViaSelection(
                                            selection = Selection(
                                                quizIds = quizzes.map(PractisoOption::id).toSet()
                                            ),
                                            type = SessionCreator.ViaSelection.Type.RecentlyCreated,
                                            preview = if (quizzes.size > 1) {
                                                getString(
                                                    Res.string.x_and_n_more_para,
                                                    firstName,
                                                    quizzes.size - 1
                                                )
                                            } else {
                                                firstName
                                            }
                                        )
                                    )
                                }

                                dimensions.forEach {
                                    add(
                                        SessionCreator.ViaSelection(
                                            selection = Selection(
                                                dimensionIds = setOf(it.dimension.id)
                                            ),
                                            type = SessionCreator.ViaSelection.Type.RecentlyCreated,
                                            preview = getPluralString(
                                                Res.plurals.n_questions_in_dimension,
                                                it.quizCount,
                                                it.quizCount,
                                                it.dimension.name
                                            )
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