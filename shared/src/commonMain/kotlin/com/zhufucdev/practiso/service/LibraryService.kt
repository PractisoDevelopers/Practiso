package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.DimensionQuizzes
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.getQuizIntensitiesById
import com.zhufucdev.practiso.datamodel.toOption
import com.zhufucdev.practiso.datamodel.toOptionFlow
import com.zhufucdev.practiso.datamodel.toTemplateOptionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryService(private val db: AppDatabase = Database.app) {
    fun getTemplates() =
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toTemplateOptionFlow()

    fun getQuizzes() =
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()

    fun getDimensions() =
        db.dimensionQueries.getAllDimensionsWithQuizCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow()

    fun getSessions() =
        db.sessionQueries.getAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.sessionQueries)

    fun getRecentTakes() =
        db.sessionQueries.getRecentTakeStats(5)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.filter { it.pinned == 1L } + it.filter { it.pinned == 0L } }

    fun getQuizFrames(quizId: Long) =
        db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(quizId)).map { it.firstOrNull() }

    fun getQuizOption(quizId: Long) = getQuizFrames(quizId).map { it?.toOption() }

    fun getQuizIntensities(dimId: Long) =
        db.dimensionQueries.getQuizIntensitiesById(dimId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun getDimensionQuizzes(): Flow<List<DimensionQuizzes>> =
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { dimensions ->
                dimensions.map {
                    DimensionQuizzes(
                        dimension = it,
                        quizzes =
                            db.dimensionQueries.getQuizzesByDimenionId(it.id)
                                .executeAsList()
                    )
                }
            }

}