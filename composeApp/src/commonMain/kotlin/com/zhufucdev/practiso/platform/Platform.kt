package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import okio.FileSystem
import okio.Path

interface Platform {
    val name: String
    val resourcePath: Path
    val filesystem: FileSystem
    fun createDbDriver(): SqlDriver
}

expect fun getPlatform(): Platform