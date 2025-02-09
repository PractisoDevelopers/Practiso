package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.RemoveService
import kotlinx.coroutines.runBlocking

class RemoveServiceSync(private val db: AppDatabase) {
    val service = RemoveService(db)
    fun removeQuizWithResources(id: Long) {
        runBlocking {
            service.removeQuizWithResources(id)
        }
    }

    fun removeDimensionKeepQuizzes(id: Long) {
        runBlocking {
            service.removeDimensionKeepQuizzes(id)
        }
    }

    fun removeDimensionWithQuizzes(id: Long) {
        runBlocking {
            service.removeDimensionWithQuizzes(id)
        }
    }
}