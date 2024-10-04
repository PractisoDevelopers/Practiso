package com.zhufucdev.practiso

import android.os.Build
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.zhufucdev.practiso.database.AppDatabase

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override suspend fun createDbDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, MainActivity.contextChan.receive(), "practiso.db")
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()