package com.zhufucdev.practiso

import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.RemoveService
import kotlinx.coroutines.runBlocking

class RemoveServiceSync(private val db: AppDatabase = Database.app) {
    val service = RemoveService(db)

    @Throws(SQLiteException::class)
    fun removeQuizWithResources(id: Long) = runBlocking {
        db.transaction {
            service.removeQuizWithResources(id)
        }
    }

    @Throws(SQLiteException::class)
    fun removeDimensionKeepQuizzes(id: Long) = runBlocking {
        service.removeDimensionKeepQuizzes(id)
    }

    @Throws(SQLiteException::class)
    fun removeDimensionWithQuizzes(id: Long) = runBlocking {
        service.removeDimensionWithQuizzes(id)
    }

    @Throws(SQLiteException::class)
    fun removeSession(id: Long) = runBlocking {
        service.removeSession(id)
    }
}