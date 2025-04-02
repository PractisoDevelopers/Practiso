package com.zhufucdev.practiso

import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.datamodel.PractisoAnswer
import com.zhufucdev.practiso.service.TakeService
import kotlinx.coroutines.runBlocking

class TakeServiceSync(private val base: TakeService) {
    @Throws(SQLiteException::class)
    fun commitAnswer(model: PractisoAnswer, priority: Int) = runBlocking {
        base.commitAnswer(model, priority)
    }

    @Throws(SQLiteException::class)
    fun rollbackAnswer(model: PractisoAnswer) = runBlocking {
        base.rollbackAnswer(model)
    }

    @Throws(SQLiteException::class)
    fun updateVisibility(hidden: Boolean) = runBlocking {
        base.updateVisibility(hidden)
    }
}