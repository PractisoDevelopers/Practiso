package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.SessionQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
sealed interface Answer {
    val quizId: Long

    fun commit(db: AppDatabase, takeId: Long, priority: Int)

    @Serializable
    data class Text(val text: String, override val quizId: Long) : Answer {
        override fun commit(db: AppDatabase, takeId: Long, priority: Int) {
            db.sessionQueries.setQuizTakeAnswer(
                quizId,
                takeId,
                answerOptionId = null,
                answerText = text,
                priority = priority.toLong()
            )
        }
    }

    @Serializable
    data class Option(val optionId: Long, override val quizId: Long) : Answer {
        override fun commit(db: AppDatabase, takeId: Long, priority: Int) {
            db.sessionQueries.setQuizTakeAnswer(
                quizId,
                takeId,
                answerOptionId = optionId,
                answerText = null,
                priority = priority.toLong()
            )
        }
    }
}

fun SessionQueries.getAnswersDataModel(takeId: Long): Flow<List<Answer>> =
    getAnswersByTakeId(takeId) { _, quizId, answerOptionId, answerText, priority ->
        when {
            answerText != null -> Answer.Text(answerText, quizId)
            answerOptionId != null -> Answer.Option(answerOptionId, quizId)
            else -> error("Either answer option nor text is present. This database is so broken.")
        }
    }
        .asFlow()
        .mapToList(Dispatchers.IO)