package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.datamodel.FramedQuiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.empty_span
import practiso.composeapp.generated.resources.n_questions_span
import practiso.composeapp.generated.resources.new_question_span

private typealias DbQuiz = com.zhufucdev.practiso.database.Quiz
private typealias DbDimension = com.zhufucdev.practiso.database.Dimension

sealed interface PractisoOption {
    @Composable
    fun titleText(): String

    @Composable
    fun previewText(): String

    data class Quiz(val quiz: DbQuiz, val preview: String?) : PractisoOption {
        @Composable
        override fun titleText(): String {
            return quiz.name ?: stringResource(Res.string.new_question_span)
        }

        @Composable
        override fun previewText(): String {
            return preview ?: stringResource(Res.string.empty_span)
        }
    }

    data class Dimension(val dimension: DbDimension, val quizCount: Int) : PractisoOption {
        @Composable
        override fun titleText(): String {
            return dimension.name
        }

        @Composable
        override fun previewText(): String =
            if (quizCount > 0)
                pluralStringResource(
                    Res.plurals.n_questions_span,
                    quizCount,
                    quizCount
                )
            else stringResource(Res.string.empty_span)
    }
}

fun Flow<List<FramedQuiz>>.toOptionFlow(): Flow<List<PractisoOption.Quiz>> =
    map { frames ->
        coroutineScope {
            frames.map {
                async {
                    PractisoOption.Quiz(
                        quiz = it.quiz,
                        preview = it.frames.firstOrNull()?.getPreviewText()
                    )
                }
            }.awaitAll()
        }
    }

fun Flow<List<DbDimension>>.toOptionFlow(db: QuizQueries): Flow<List<PractisoOption.Dimension>> =
    map { dimensions ->
        coroutineScope {
            dimensions.map {
                async {
                    PractisoOption.Dimension(
                        dimension = it,
                        quizCount = db.getQuizCountByDimension(it.id)
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .map { it.first().toInt() }
                            .last()
                    )
                }
            }.awaitAll()
        }
    }