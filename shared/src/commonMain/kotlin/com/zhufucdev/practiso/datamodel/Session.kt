package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.database.Answer
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.GetAllAnswers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
fun calculateTakeNumber(db: AppDatabase, takeId: Long): Flow<Int> =
    db.sessionQueries.getTakeById(takeId)
        .asFlow()
        .mapToOne(Dispatchers.IO)
        .flatMapConcat { take ->
            db.sessionQueries
                .getTakeStatsBySessionId(take.sessionId)
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { stats ->
                    stats.indexOfFirst { stat -> stat.id == takeId } + 1
                }
        }

fun calculateTakeCorrectQuizCount(db: AppDatabase, takeId: Long): Flow<Int> =
    db.quizQueries.getQuizFrames(db.sessionQueries.getQuizzesByTakeId(takeId))
        .combine(db.sessionQueries.getAnswersDataModel(takeId), ::Pair)
        .map { (quizzes, answers) -> quizzes.count { isAnsweredCorrectly(it, answers) } }

fun isAnsweredCorrectly(quiz: QuizFrames, answers: List<PractisoAnswer>): Boolean {
    val answerables =
        quiz.frames.map(PrioritizedFrame::frame).filterIsInstance<Frame.Answerable<*>>()
    return answerables.all { frame ->
        val current = answers.filter { a -> a.frameId == frame.id }
        when (frame) {
            is Frame.Options -> with(frame) {
                @Suppress("UNCHECKED_CAST")
                (current as List<PractisoAnswer.Option>).isAdequateNecessary()
            }
        }
    }
}

data class AnsweredQuiz(val quiz: QuizFrames, val answers: List<Answer>, val isCorrect: Boolean)

fun AppDatabase.getAllAnsweredQuizzes(): Flow<List<AnsweredQuiz>> =
    sessionQueries.getAllAnswers()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map {
            it.groupBy { q -> q.quizId to q.takeId }
                .mapNotNull { (comb, answers) ->
                    val (quizId, _) = comb
                    quizQueries.getQuizFrames(quizQueries.getQuizById(quizId))
                        .first().firstOrNull()
                        ?.let { quiz ->
                            val answers = answers.map { ans -> ans.toAnswer() }
                            AnsweredQuiz(
                                quiz,
                                answers,
                                isAnsweredCorrectly(quiz, answers.map(Answer::toDataModel))
                            )
                        }
                }
        }

fun GetAllAnswers.toAnswer() =
    Answer(takeId, quizId, optionsFrameId, textFrameId, answerOptionId, answerText, time, priority)
