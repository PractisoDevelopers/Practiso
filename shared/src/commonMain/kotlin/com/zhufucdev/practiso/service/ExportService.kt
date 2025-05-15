package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.DimensionArchive
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.QuizArchive
import com.zhufucdev.practiso.datamodel.archive
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import okio.Source

class ExportService(private val db: AppDatabase = Database.app) {
    suspend fun exportAsSource(quizIds: Collection<Long>): Source {
        val platform = getPlatform()
        return db.quizQueries
            .getQuizFrames(db.quizQueries.getQuizByIds(quizIds))
            .first()
            .map {
                val dimensions = db.dimensionQueries.getDimensionByQuizId(it.quiz.id).executeAsList()
                QuizArchive(
                    name = it.quiz.name ?: "",
                    creationTime = Clock.System.now(),
                    frames = it.frames.sortedBy(PrioritizedFrame::priority).map { it.frame.toArchive() },
                    dimensions = dimensions.map { DimensionArchive(it.name, it.intensity) }
                )
            }
            .archive {
                platform.filesystem.source(platform.resourcePath.resolve(it))
            }
    }
}