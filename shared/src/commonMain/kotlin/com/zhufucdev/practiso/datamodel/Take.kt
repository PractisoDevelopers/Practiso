package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Clock
import kotlin.time.Duration

fun calculateTakeNumber(db: AppDatabase, takeId: Long): Flow<Int> = channelFlow {
    db.sessionQueries.getTakeById(takeId)
        .asFlow()
        .mapToOne(Dispatchers.IO)
        .collectLatest { take ->
            db.sessionQueries
                .getTakeStatsBySessionId(take.sessionId)
                .asFlow()
                .mapToList(Dispatchers.IO)
                .collect { stats ->
                    send(stats.indexOfFirst { stat -> stat.id == takeId } + 1)
                }
        }
}

fun calculateTakeCorrectQuizCount(db: AppDatabase, takeId: Long): Flow<Int> = channelFlow {
    db.quizQueries.getQuizFrames(db.sessionQueries.getQuizzesByTakeId(takeId))
        .collectLatest { quizzes ->
            db.sessionQueries.getAnswersDataModel(takeId)
                .collect { answers ->
                    send(calculateCorrectness(quizzes, answers))
                }
        }
}

private fun calculateCorrectness(quizzes: List<QuizFrames>, answers: List<Answer>): Int =
    quizzes.count { option ->
        val answerables =
            option.frames.map(PrioritizedFrame::frame).filterIsInstance<Frame.Answerable<*>>()
        answerables.all { frame ->
            val current = answers.filter { a -> a.frameId == frame.id }
            when (frame) {
                is Frame.Options -> with(frame) {
                    @Suppress("UNCHECKED_CAST")
                    (current as List<Answer.Option>).isAdequateNecessary()
                }
            }
        }
    }

suspend fun createTake(sessionId: Long, timers: List<Duration>, db: AppDatabase): Long {
    val takeId = db.transactionWithResult {
        db.sessionQueries.updateSessionAccessTime(
            Clock.System.now(),
            sessionId
        )
        db.sessionQueries.insertTake(
            sessionId = sessionId,
            creationTimeISO = Clock.System.now(),
        )
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
