package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toQuizOptionFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.time.Duration

class CreateService(private val db: AppDatabase = Database.app) {
    suspend fun createNewQuiz(): QuizOption {
        val id = db.transactionWithResult {
            db.quizQueries.insertQuiz(null, Clock.System.now(), null)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }
        return db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(id))
            .toQuizOptionFlow()
            .first()
            .first()
    }

    suspend fun createSession(name: String, selection: Selection): Long {
        val sessionId = db.transactionWithResult {
            db.sessionQueries.insertSesion(name)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        val quizIdsByDimensions = selection.dimensionIds.map {
            db.quizQueries.getQuizByDimension(it).executeAsList().map(Quiz::id)
        }.flatten()

        val quizzes = selection.quizIds + quizIdsByDimensions
        if (quizzes.isEmpty()) {
            throw NullPointerException("session would be empty")
        }
        db.transaction {
            quizzes.forEach {
                db.sessionQueries.assoicateQuizWithSession(it, sessionId)
            }
        }

        return sessionId
    }

    suspend fun createTake(sessionId: Long, timers: List<Duration>): Long {
        val takeId = db.transactionWithResult {
            db.sessionQueries.updateSessionAccessTime(
                Clock.System.now(),
                sessionId
            )
            db.sessionQueries.insertTake(sessionId)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        db.transaction {
            timers.forEach { d ->
                db.sessionQueries.associateTimerWithTake(
                    takeId,
                    durationSeconds = d.inWholeMilliseconds / 1000.0
                )
            }
        }

        return takeId
    }
}