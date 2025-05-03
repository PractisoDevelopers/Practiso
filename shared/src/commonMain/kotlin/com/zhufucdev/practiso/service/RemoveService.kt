package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class RemoveService(private val db: AppDatabase) {
    suspend fun removeQuizWithResources(id: Long) {
        val quizFrames = db.quizQueries
            .getQuizFrames(db.quizQueries.getQuizById(id))
            .firstOrNull()
        if (quizFrames.isNullOrEmpty()) {
            return
        }

        withContext(Dispatchers.IO) {
            val platform = getPlatform()
            quizFrames.first()
                .frames
                .map(PrioritizedFrame::frame)
                .resources()
                .map { (name) ->
                    async {
                        platform.filesystem.delete(
                            platform.resourcePath.resolve(
                                name
                            )
                        )
                    }
                }
                .awaitAll()
        }

        db.transaction {
            db.quizQueries.removeQuiz(id)
        }
    }

    suspend fun removeDimensionKeepQuizzes(id: Long) {
        db.transaction {
            db.dimensionQueries.removeDimension(id)
        }
    }

    suspend fun removeDimensionWithQuizzes(id: Long) {
        db.transaction {
            db.quizQueries.getQuizByDimension(id)
                .executeAsList()
                .forEach {
                    db.quizQueries.removeQuiz(it.id)
                }
            db.dimensionQueries.removeDimension(id)
        }
    }

    suspend fun removeSession(id: Long) {
        db.transaction {
            db.sessionQueries.removeSession(id)
        }
    }
}