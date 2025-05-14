package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.GetAllDimensionsWithQuizCount
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Template
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias DbQuiz = Quiz
private typealias DbDimension = Dimension
private typealias DbSessionOption = com.zhufucdev.practiso.database.SessionOptionView

sealed interface PractisoOption {
    val id: Long
}

data class QuizOption(val quiz: DbQuiz, val preview: String?) : PractisoOption {
    override val id: Long
        get() = quiz.id
}

data class DimensionOption(val dimension: DbDimension, val quizCount: Int) : PractisoOption,
    SessionCreator {
    override val id: Long
        get() = dimension.id
    override val selection: Selection
        get() = Selection(dimensionIds = setOf(id))
}

data class SessionOption(val session: Session, val quizCount: Int) : PractisoOption {
    override val id: Long
        get() = session.id
}

data class TemplateOption(val template: Template) : PractisoOption {
    override val id: Long
        get() = template.id
}

suspend fun QuizFrames.toOption() = coroutineScope {
    QuizOption(
        quiz = quiz,
        preview = frames.map { async { it.frame.getPreviewText() } }.awaitAll()
            .joinToString("  ")
    )
}

fun Flow<List<QuizFrames>>.toQuizOptionFlow(): Flow<List<QuizOption>> =
    map { frames ->
        coroutineScope {
            frames.map {
                async { it.toOption() }
            }.awaitAll()
        }
    }

fun Flow<List<DbDimension>>.toDimensionOptionFlow(db: QuizQueries): Flow<List<DimensionOption>> =
    map { dimensions ->
        coroutineScope {
            dimensions.map {
                async {
                    DimensionOption(
                        dimension = it,
                        quizCount = (db.getQuizCountByDimension(it.id)
                            .executeAsOneOrNull() ?: 0)
                            .toInt()
                    )
                }
            }.awaitAll()
        }
    }

fun Flow<List<GetAllDimensionsWithQuizCount>>.toDimensionOptionFlow(): Flow<List<DimensionOption>> =
    map { data ->
        data.map {
            DimensionOption(
                dimension = Dimension(it.id, it.name),
                quizCount = it.quizCount.toInt()
            )
        }
    }

fun DbSessionOption.toOption() =
    SessionOption(
        session = Session(
            id = id,
            name = name,
            creationTimeISO = creationTimeISO,
            lastAccessTimeISO = lastAccessTimeISO
        ),
        quizCount = quizCount.toInt()
    )

fun Flow<List<DbSessionOption>>.toSessionOptionFlow(): Flow<List<SessionOption>> =
    map { sessions ->
        sessions.map(DbSessionOption::toOption)
    }

fun Flow<List<Template>>.toTemplateOptionFlow() = map { it.map { t -> TemplateOption(t) } }

