package com.zhufucdev.practiso

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toOptionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map

class LibraryService(private val db: AppDatabase = Database.app) {
    fun getTemplates() =
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow()

    fun getQuizzes() =
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()

    fun getDimensions() =
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.quizQueries)

    fun getQuizFrames(quizId: Long) =
        db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(quizId)).map { it.firstOrNull() }
}