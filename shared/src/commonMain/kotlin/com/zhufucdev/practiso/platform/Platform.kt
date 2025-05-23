package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import okio.FileSystem
import okio.Path

abstract class Platform {
    abstract val name: String
    abstract val resourcePath: Path
    abstract val filesystem: FileSystem
    abstract val settingsFactory: Settings.Factory
    abstract val logicalProcessorsCount: Int
    abstract fun createDbDriver(): SqlDriver
    abstract fun getInferenceSession(): Flow<InferenceSession>

    val defaultSettings: Settings by lazy {
        settingsFactory.create()
    }
}

expect fun getPlatform(): Platform