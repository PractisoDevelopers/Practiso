package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Selection(
    val quizIds: Set<Long> = emptySet(),
    val dimensionIds: Set<Long> = emptySet(),
)

/**
 * Create a new session
 * @return session id
 */
suspend fun createSession(name: String, selection: Selection, db: AppDatabase): Long {
    val sessionId = db.transactionWithResult {
        db.sessionQueries.insertSession(name, Clock.System.now())
        db.quizQueries.lastInsertRowId().executeAsOne()
    }

    val quizIdsByDimensions = selection.dimensionIds.map {
        db.quizQueries.getQuizByDimension(it).executeAsList().map(Quiz::id)
    }.flatten()

    val quizzes = selection.quizIds + quizIdsByDimensions
    db.transaction {
        quizzes.forEach {
            db.sessionQueries.assoicateQuizWithSession(it, sessionId)
        }
    }

    return sessionId
}