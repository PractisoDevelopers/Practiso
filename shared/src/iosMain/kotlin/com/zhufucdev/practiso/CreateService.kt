package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toOptionFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class CreateService(private val db: AppDatabase) {
    fun createNewQuiz(): QuizOption =
        runBlocking {
            val id = db.transactionWithResult {
                db.quizQueries.insertQuiz(null, Clock.System.now(), null)
                db.quizQueries.lastInsertRowId().executeAsOne()
            }
            db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(id))
                .toOptionFlow()
                .first()
                .first()
        }
}