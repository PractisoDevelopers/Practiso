package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.datamodel.QuizFrames
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.empty_span
import practiso.composeapp.generated.resources.n_questions_span
import practiso.composeapp.generated.resources.new_question_para

private typealias DbQuiz = com.zhufucdev.practiso.database.Quiz
private typealias DbDimension = com.zhufucdev.practiso.database.Dimension

sealed interface PractisoOption {
    @Composable
    fun titleString(): String

    @Composable
    fun previewString(): String

    data class Quiz(val quiz: DbQuiz, val preview: String?) : PractisoOption {
        @Composable
        override fun titleString(): String {
            return quiz.name?.takeIf(String::isNotEmpty)
                ?: stringResource(Res.string.new_question_para)
        }

        @Composable
        override fun previewString(): String {
            return preview ?: stringResource(Res.string.empty_span)
        }
    }

    data class Dimension(val dimension: DbDimension, val quizCount: Int) : PractisoOption {
        @Composable
        override fun titleString(): String {
            return dimension.name
        }

        @Composable
        override fun previewString(): String =
            if (quizCount > 0)
                pluralStringResource(
                    Res.plurals.n_questions_span,
                    quizCount,
                    quizCount
                )
            else stringResource(Res.string.empty_span)
    }
}

fun Flow<List<QuizFrames>>.toOptionFlow(): Flow<List<PractisoOption.Quiz>> =
    map { frames ->
        coroutineScope {
            frames.map {
                async {
                    PractisoOption.Quiz(
                        quiz = it.quiz,
                        preview = it.frames.map { async { it.frame.getPreviewText() } }.awaitAll()
                            .joinToString(" ")
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
                            .executeAsOne()
                            .toInt()
                    )
                }
            }.awaitAll()
        }
    }