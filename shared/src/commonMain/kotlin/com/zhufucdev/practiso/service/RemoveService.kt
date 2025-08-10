package com.zhufucdev.practiso.service

import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull

class RemoveService(private val db: AppDatabase = Database.app) {
    suspend fun removeQuizWithResources(id: Collection<Long>) {
        val quizFrames = db.quizQueries
            .getQuizFrames(db.quizQueries.getQuizByIds(id))
            .firstOrNull()
        if (quizFrames.isNullOrEmpty()) {
            return
        }

        coroutineScope {
            val platform = getPlatform()
            quizFrames.map(QuizFrames::frames)
                .flatten()
                .map(PrioritizedFrame::frame)
                .resources()
                .map { (name) ->
                    async(Dispatchers.IO) {
                        platform.filesystem.delete(
                            platform.resourcePath.resolve(
                                name
                            )
                        )
                    }
                }
                .awaitAll()
        }

        db.quizQueries.removeQuizzesWithFrames(id).awaitAsOne()
    }

    suspend fun removeQuizWithResources(id: Long) {
        removeQuizWithResources(listOf(id))
    }

    suspend fun removeDimensionKeepQuizzes(id: Long) {
        db.transaction {
            db.dimensionQueries.removeDimension(id)
        }
    }

    suspend fun removeDimensionKeepQuizzes(ids: Collection<Long>) {
        db.transaction {
            db.dimensionQueries.removeDimensions(ids)
        }
    }

    suspend fun removeDimensionWithQuizzes(id: Long) {
        db.transaction {
            val quizIds = db.quizQueries.getQuizByDimensions(listOf(id))
                .executeAsList()
                .map(Quiz::id)

            db.quizQueries.removeQuizzesWithFrames(quizIds).awaitAsOne()
            db.dimensionQueries.removeDimension(id)
        }
    }

    suspend fun removeDimensionWithQuizzes(ids: Collection<Long>) {
        db.transaction {
            val quizIds = db.quizQueries.getQuizByDimensions(ids)
                .executeAsList()
                .map(Quiz::id)

            db.quizQueries.removeQuizzesWithFrames(quizIds).awaitAsOne()
            db.dimensionQueries.removeDimensions(ids)
        }
    }

    suspend fun removeSession(id: Long) {
        db.transaction {
            db.sessionQueries.removeSession(id)
        }
    }
}