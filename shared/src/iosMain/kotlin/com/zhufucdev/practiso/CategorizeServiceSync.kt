package com.zhufucdev.practiso

import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.CategorizeService
import kotlinx.coroutines.runBlocking

class CategorizeServiceSync(db: AppDatabase) {
    private val service = CategorizeService(db)

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
}