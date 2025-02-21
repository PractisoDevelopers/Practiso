package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.CategorizeService
import kotlinx.coroutines.runBlocking

class CategorizeServiceSync(db: AppDatabase) {
    private val service = CategorizeService(db)

    fun associate(quizId: Long, dimensionId: Long) = runBlocking {
        service.associate(quizId, dimensionId)
    }

    fun disassociate(quizId: Long, dimensionId: Long) = runBlocking {
        service.disassociate(quizId, dimensionId)
    }
}