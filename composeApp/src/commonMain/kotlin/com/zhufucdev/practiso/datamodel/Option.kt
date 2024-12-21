package com.zhufucdev.practiso.datamodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.SessionQueries
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.empty_span
import practiso.composeapp.generated.resources.n_questions_dot_created_date_para
import practiso.composeapp.generated.resources.n_questions_span
import practiso.composeapp.generated.resources.new_question_para

private typealias DbQuiz = Quiz
private typealias DbDimension = Dimension
private typealias DbSession = Session

sealed interface PractisoOption {
    val id: Long

    @Composable
    fun titleString(): String

    @Composable
    fun previewString(): String

    data class Quiz(val quiz: DbQuiz, val preview: String?) : PractisoOption {
        override val id: Long
            get() = quiz.id

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

    data class Dimension(val dimension: DbDimension, val quizCount: Int) : PractisoOption,
        SessionCreator {
        override val id: Long
            get() = dimension.id

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

        override val selection: Selection
            get() = Selection(dimensionIds = setOf(id))
        override val sessionName: String?
            get() = dimension.name
    }

    data class Session(val session: DbSession, val quizCount: Int) : PractisoOption {
        override val id: Long
            get() = session.id

        @Composable
        override fun titleString(): String {
            return session.name
        }

        @Composable
        override fun previewString(): String {
            return pluralStringResource(
                Res.plurals.n_questions_dot_created_date_para,
                quizCount,
                quizCount,
                HumanReadable.timeAgo(session.creationTimeISO)
            )
        }
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
                            .joinToString("  ")
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
                        quizCount = (db.getQuizCountByDimension(it.id)
                            .executeAsOneOrNull() ?: 0)
                            .toInt()
                    )
                }
            }.awaitAll()
        }
    }

fun Flow<List<DbSession>>.toOptionFlow(db: SessionQueries): Flow<List<PractisoOption.Session>> =
    map { sessions ->
        coroutineScope {
            sessions.map {
                async {
                    PractisoOption.Session(
                        session = it,
                        quizCount = db.getQuizCountBySession(it.id)
                            .executeAsOne()
                            .toInt()
                    )
                }
            }.awaitAll()
        }
    }