package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.getQuizByIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.math.max

class ClusterService(private val db: AppDatabase, private val fei: FeiService) {
    fun cluster(dimensionId: Long, minSimilarity: Float = 0.6f): Flow<ClusterState> = flow {
        emit(ClusterState.Preparing)
        val fei = fei.getUpgradeState()
            .filterIsInstance<FeiDbState.Ready>()
            .first()
        val embedding = fei.inference.model.embeddingOutput
            ?: error("Missing model because the current one doesn't support embedding output.")

        val dimensionName = db.dimensionQueries.getDimensionById(dimensionId)
            .executeAsOne()
            .name

        emit(ClusterState.Inference(dimensionName))
        val targetEbd = fei.inference.getEmbeddings(
            Frame.Text(textFrame = TextFrame(id = -1, content = dimensionName))
        )

        emit(ClusterState.Search)
        val totalQuizzes = db.quizQueries.getQuizCount().executeAsOne()
        val insertion = coroutineScope {
            withContext(Dispatchers.Default) {
                fei.index.search(
                    targetEbd,
                    max((totalQuizzes / 2).toInt(), 10)
                )
            }
                .mapNotNull { search ->
                    val sim = embedding.normalizer(search.distance)
                    if (sim >= minSimilarity) {
                        async(Dispatchers.IO) {
                            db.quizQueries.getQuizByIndex(
                                db.embeddingQueries.getIndexByKey(search.key.toLong())
                                    .executeAsOne()
                            ).executeAsOne().id to sim
                        }
                    } else {
                        null
                    }
                }
                .awaitAll()
                .groupingBy { it.first }
                .reduce { quizId, a, b -> a.takeIf { it.second > b.second } ?: b }
        }
        db.transaction {
            insertion.forEach { (quizId, sim) ->
                db.dimensionQueries.associateQuizWithDimension(
                    quizId,
                    dimensionId,
                    sim.second.toDouble()
                )
            }
        }

        emit(ClusterState.Complete(found = insertion.size))
    }
}