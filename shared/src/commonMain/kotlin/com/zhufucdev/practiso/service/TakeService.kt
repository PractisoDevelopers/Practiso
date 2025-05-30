package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Take
import com.zhufucdev.practiso.database.TimerByTake
import com.zhufucdev.practiso.datamodel.PractisoAnswer
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.calculateTakeNumber
import com.zhufucdev.practiso.datamodel.getAnswersDataModel
import com.zhufucdev.practiso.datamodel.getQuizFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.random.Random

class TakeService(private val takeId: Long, private val db: AppDatabase = Database.app) {
    fun getTake(): Flow<Take> =
        db.sessionQueries.getTakeById(takeId)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun getQuizzes(): Flow<List<QuizFrames>> {
        val creationTime = db.sessionQueries.getTakeById(takeId).executeAsOne().creationTimeISO
        return db.quizQueries.getQuizFrames(db.sessionQueries.getQuizzesByTakeId(takeId))
            .map { frames -> frames.shuffled(Random(creationTime.epochSeconds)) }
    }

    suspend fun getCurrentQuiz(): QuizFrames? {
        val id = getCurrentQuizId()
        val quizzes = getQuizzes().first()
        return if (id == null) {
            quizzes.firstOrNull()
        } else {
            quizzes.firstOrNull { it.quiz.id == id }
        }
    }

    fun getCurrentQuizId(): Long? =
        db.sessionQueries.getCurrentQuizIdByTakeId(takeId)
            .executeAsOne()
            .currentQuizId

    fun getTakeNumber(): Flow<Int> =
        calculateTakeNumber(db, takeId)

    fun getSession(): Flow<Session> =
        db.sessionQueries.getSessionByTakeId(takeId)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun getTimersInSecond(): Flow<List<Double>> =
        db.sessionQueries
            .getTimersByTakeId(takeId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { t -> t.map(TimerByTake::durationSeconds) }

    fun getAnswers(): Flow<List<PractisoAnswer>> =
        db.sessionQueries.getAnswersDataModel(takeId)

    suspend fun updateAccessTime() {
        db.transaction {
            db.sessionQueries.updateTakeAccessTime(Clock.System.now(), takeId)
            db.sessionQueries.updateSessionAccessTimeByTakeId(Clock.System.now(), takeId)
        }
    }

    suspend fun updateDuration(durationInSeconds: Long) {
        db.transaction {
            db.sessionQueries.updateTakeDuration(durationInSeconds, takeId)
        }
    }

    suspend fun commitAnswer(model: PractisoAnswer, priority: Int) {
        db.transaction {
            model.commit(db, takeId, priority)
        }
    }

    suspend fun rollbackAnswer(model: PractisoAnswer) {
        db.transaction {
            model.rollback(db, takeId)
        }
    }

    suspend fun updateCurrentQuizId(currentQuizId: Long) {
        db.transaction {
            db.sessionQueries.updateCurrentQuizId(currentQuizId, takeId)
        }
    }

    suspend fun updateVisibility(hidden: Boolean) {
        db.transaction {
            db.sessionQueries.updateTakeVisibility(if (hidden) 1 else 0, takeId)
        }
    }
}