package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.database.AppDatabase

class CategorizeService(private val db: AppDatabase) {
    suspend fun associate(quizId: Long, dimensionId: Long) {
        db.dimensionQueries.associateQuizWithDimension(quizId, dimensionId, 1.0)
    }

    suspend fun disassociate(quizId: Long, dimensionId: Long) {
        db.dimensionQueries.dissoicateQuizFromDimension(quizId, dimensionId)
    }

    suspend fun updateIntensity(quizId: Long, dimensionId: Long, value: Double) {
        db.dimensionQueries.updateDimensionAssoicationIntensity(value, quizId, dimensionId)
    }
}