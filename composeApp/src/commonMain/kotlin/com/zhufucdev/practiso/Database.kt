package com.zhufucdev.practiso

import app.cash.sqldelight.db.SqlDriver
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.platform.getPlatform

object Database {
    private val driver: SqlDriver by lazy {
        getPlatform().createDbDriver()
    }

    val app: AppDatabase by lazy {
        driver.toDatabase()
    }

    suspend fun migrate(): Boolean {
        val newVersion = AppDatabase.Schema.version
        val currentVersion = AppSettings.databaseVersion.value ?: newVersion
        if (currentVersion >= newVersion) {
            AppSettings.databaseVersion.emit(newVersion)
            return false
        }

        AppDatabase.Schema.migrate(
            driver = driver,
            oldVersion = currentVersion,
            newVersion = newVersion
        ).await()
        AppSettings.databaseVersion.emit(newVersion)

        return true
    }
}
