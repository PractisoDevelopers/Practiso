package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.database.AppDatabase

class CategorizeService(private val db: AppDatabase) {
    suspend fun associate(quizId: Long, dimensionId: Long, intensity: Double = 1.0) {
        db.dimensionQueries.associateQuizWithDimension(quizId, dimensionId, 1.0)
    }

    suspend fun associate(quizIds: List<Long>, dimensionId: Long, intensities: DoubleArray? = null) {
        if (intensities != null && quizIds.size != intensities.size) {
            throw IllegalArgumentException("The quiz list doesn't contain the same amount of items as the intensity list.")
        }
        db.transaction {
            if (intensities != null) {
                for (idx in quizIds.indices) {
                    db.dimensionQueries.associateQuizWithDimension(
                        quizIds[idx],
                        dimensionId,
                        intensities[idx]
                    )
                }
            } else {
                for (id in quizIds) {
                    db.dimensionQueries.associateQuizWithDimension(id, dimensionId, 1.0)
                }
            }
        }
    }

    suspend fun disassociate(quizId: Long, dimensionId: Long) {
        db.dimensionQueries.dissoicateQuizFromDimension(quizId, dimensionId)
    }

    suspend fun disassociate(quizIds: Collection<Long>, dimensionId: Long) {
        db.dimensionQueries.dissoicateQuizzesFromDimension(quizIds, dimensionId)
    }

    suspend fun updateIntensity(quizId: Long, dimensionId: Long, value: Double) {
        db.dimensionQueries.updateDimensionAssoicationIntensity(value, quizId, dimensionId)
    }

    suspend fun createDimension(name: String): Long {
        val existing = db.dimensionQueries.getDimensionByName(name).executeAsOneOrNull()
        if (existing == null) {
            return db.transactionWithResult {
                db.dimensionQueries.insertDimension(name)
                db.quizQueries.lastInsertRowId().executeAsOne()
            }
        }
        return existing.id
    }
}

sealed class ClusterState {
    data class Complete(val found: Int) : ClusterState()
    data class Inference(val text: String) : ClusterState()
    data object Preparing : ClusterState()
    data object Search : ClusterState()
}