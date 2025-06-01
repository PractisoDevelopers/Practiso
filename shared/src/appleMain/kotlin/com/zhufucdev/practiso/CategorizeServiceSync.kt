package com.zhufucdev.practiso

import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.CategorizeService
import com.zhufucdev.practiso.service.ClusterState
import com.zhufucdev.practiso.service.FeiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class CategorizeServiceSync(db: AppDatabase, fei: FeiService) {
    private val service = CategorizeService(db, fei)

    @Throws(SQLiteException::class)
    fun associate(quizId: Long, dimensionId: Long) = runBlocking {
        service.associate(quizId, dimensionId)
    }

    fun disassociate(quizId: Long, dimensionId: Long) = runBlocking {
        service.disassociate(quizId, dimensionId)
    }

    fun updateIntensity(quizId: Long, dimensionId: Long, value: Double) = runBlocking {
        service.updateIntensity(quizId, dimensionId, value)
    }

    fun createDimension(name: String) = runBlocking {
        service.createDimension(name)
    }


    fun cluster(dimensionId: Long, minSimilarity: Float = 0.6f): Flow<ClusterState> =
        service.cluster(dimensionId, minSimilarity)
}
