package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import okio.FileSystem
import okio.Path

abstract class Platform {
    abstract val name: String
    abstract val resourcePath: Path
    abstract val filesystem: FileSystem
    abstract val logicalProcessorsCount: Int
    abstract fun createDbDriver(): SqlDriver
}

expect fun getPlatform(): Platform