package com.zhufucdev.practiso.platform

import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.zhufucdev.practiso.PractisoApp
import com.zhufucdev.practiso.database.AppDatabase
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

class AndroidPlatform : Platform() {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun createDbDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AppDatabase.Schema.synchronous(),
            context = PractisoApp.instance,
            name = "practiso.db",
            callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema.synchronous()) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    override val filesystem: FileSystem
        get() = FileSystem.SYSTEM

    override val settingsFactory: Settings.Factory by lazy {
        SharedPreferencesSettings.Factory(PractisoApp.instance)
    }

    override val resourcePath: Path by lazy {
        PractisoApp.instance.filesDir.toOkioPath()
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()