package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.SessionQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
sealed interface Answer {
    val quizId: Long
    val frameId: Long

    suspend fun commit(db: AppDatabase, takeId: Long, priority: Int)
    suspend fun rollback(db: AppDatabase, takeId: Long)

    @Serializable
    data class Text(val text: String, override val frameId: Long, override val quizId: Long) :
        Answer {
        override suspend fun commit(db: AppDatabase, takeId: Long, priority: Int) {
            db.sessionQueries.setTextAnswer(
                quizId,
                takeId,
                textFrameId = frameId,
                answerText = text,
                priority = priority.toLong()
            )
        }

        override suspend fun rollback(db: AppDatabase, takeId: Long) {
            db.sessionQueries.removeTextAnswer(frameId, quizId, takeId)
        }
    }

    @Serializable
    data class Option(val optionId: Long, override val frameId: Long, override val quizId: Long) :
        Answer {
        override suspend fun commit(db: AppDatabase, takeId: Long, priority: Int) {
            db.sessionQueries.setOptionAnswer(
                quizId,
                takeId,
                answerOptionId = optionId,
                optionsFrameId = frameId,
                priority = priority.toLong()
            )
        }

        override suspend fun rollback(db: AppDatabase, takeId: Long) {
            db.sessionQueries.removeOptionAnswer(
                answerOptionId = optionId,
                optionsFrameId = frameId,
                quizId = quizId,
                takeId = takeId
            )
        }
    }
}

fun SessionQueries.getAnswersDataModel(takeId: Long): Flow<List<Answer>> =
    getAnswersByTakeId(takeId)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { answers ->
            answers
                .sortedBy { it.priority }
                .map {
                    when {
                        it.textFrameId != null -> Answer.Text(
                            it.answerText!!,
                            it.textFrameId,
                            it.quizId
                        )

                        it.optionsFrameId != null -> Answer.Option(
                            it.answerOptionId!!,
                            it.optionsFrameId,
                            it.quizId
                        )

                        else -> error("Either answer option nor text is present. This database is so broken.")
                    }
                }
        }