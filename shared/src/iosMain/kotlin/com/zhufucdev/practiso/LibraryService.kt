package com.zhufucdev.practiso

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Edit
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.applyTo
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.insertInto
import com.zhufucdev.practiso.datamodel.optimized
import com.zhufucdev.practiso.datamodel.toOptionFlow
import com.zhufucdev.practiso.datamodel.toTemplateOptionFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.quizQueries)

    fun getQuizFrames(quizId: Long) =
        db.quizQueries.getQuizFrames(db.quizQueries.getQuizById(quizId)).map { it.firstOrNull() }

    @Throws(SQLiteException::class)
    fun createQuestion(frames: List<Frame>, name: String?) = runBlocking {
        frames.map(Frame::toArchive).insertInto(db, name)
    }

    @Throws(SQLiteException::class)
    fun saveEdit(data: List<Edit>, quizId: Long) = runBlocking {
        data.optimized().applyTo(db, quizId)
    }
}