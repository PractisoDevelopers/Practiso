package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

object LibraryDataModel {
    private val db: AppDatabase = Database.app

    val templates by lazy {
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow()
    }

    val quiz: Flow<List<QuizOption>> by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
    }

    val dimensions by lazy {
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.quizQueries)
    }
}