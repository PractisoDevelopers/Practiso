package com.zhufucdev.practiso

import co.touchlab.sqliter.interop.SQLiteException
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Edit
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.applyTo
import com.zhufucdev.practiso.datamodel.insertInto
import com.zhufucdev.practiso.datamodel.optimized
import kotlinx.coroutines.runBlocking

class EditService(private val db: AppDatabase) {
    @Throws(SQLiteException::class)
    fun createQuestion(frames: List<Frame>, name: String?) = runBlocking {
        frames.map(Frame::toArchive).insertInto(db, name)
    }

    @Throws(SQLiteException::class)
    fun saveEdit(data: List<Edit>, quizId: Long) = runBlocking {
        db.transaction {
            data.optimized().applyTo(db, quizId)
        }
    }

    @Throws(SQLiteException::class)
    fun renameSession(sessionId: Long, newName: String) = runBlocking {
        db.transaction {
            db.sessionQueries.renameSession(newName, sessionId)
        }
    }
}
