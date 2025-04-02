package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class QueryService(private val db: AppDatabase) {
    fun getQuizOption(quizId: Long) = runBlocking {
        db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(quizId))
            .first()
            .firstOrNull()
            ?.toOption()
    }
}