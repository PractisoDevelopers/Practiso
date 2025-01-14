package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.Settings
import com.zhufucdev.practiso.datamodel.VectorDatabaseDriver
import okio.FileSystem
import okio.Path

abstract class Platform {
    abstract val name: String
    abstract val resourcePath: Path
    abstract val filesystem: FileSystem
    abstract val settingsFactory: Settings.Factory
    abstract fun createDbDriver(): SqlDriver
    abstract fun createVectorDbDriver(): VectorDatabaseDriver

    val defaultSettings: Settings by lazy {
        settingsFactory.create()
    }
}

expect fun getPlatform(): Platform