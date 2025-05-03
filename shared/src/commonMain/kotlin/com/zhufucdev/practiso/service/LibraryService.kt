package com.zhufucdev.practiso.service

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.SessionOptionView
import com.zhufucdev.practiso.datamodel.DimensionQuizzes
import com.zhufucdev.practiso.datamodel.SessionOption
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.getQuizIntensitiesById
import com.zhufucdev.practiso.datamodel.toDimensionOptionFlow
import com.zhufucdev.practiso.datamodel.toOption
import com.zhufucdev.practiso.datamodel.toQuizOptionFlow
import com.zhufucdev.practiso.datamodel.toSessionOptionFlow
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
            .toQuizOptionFlow()

    fun getDimensions() =
        db.dimensionQueries.getAllDimensionsWithQuizCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toDimensionOptionFlow()

    fun getSessions() =
        db.sessionQueries.getAllSessionOptions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toSessionOptionFlow()

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

    fun getSession(id: Long): Flow<SessionOption> =
        db.sessionQueries.getSessionOptionById(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map(SessionOptionView::toOption)

    fun getTakesBySession(id: Long) =
        db.sessionQueries.getTakeStatsBySessionId(id)
            .asFlow()
            .mapToList(Dispatchers.IO)
}