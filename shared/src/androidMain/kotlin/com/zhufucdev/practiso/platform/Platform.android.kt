package com.zhufucdev.practiso.platform

import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.FeiService
import kotlinx.coroutines.flow.MutableStateFlow
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

object AndroidPlatform : Platform() {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun createDbDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AppDatabase.Schema.synchronous(),
            context = SharedContext,
            name = "practiso.db",
            callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema.synchronous()) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    val maxParallelInferences = MutableStateFlow(FeiService.MAX_BATCH_SIZE)

    override val filesystem: FileSystem
        get() = FileSystem.SYSTEM

    override val resourcePath: Path by lazy {
        SharedContext.filesDir.toOkioPath()
    }

    override val logicalProcessorsCount: Int
        get() = Runtime.getRuntime().availableProcessors()
}

actual fun getPlatform(): Platform = AndroidPlatform