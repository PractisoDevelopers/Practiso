package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import okio.FileSystem
import okio.Path

abstract class Platform {
    abstract val name: String
    abstract val deviceName: String
    abstract val resourcePath: Path
    abstract val filesystem: FileSystem
    abstract val logicalProcessorsCount: Int
    abstract fun createDbDriver(): SqlDriver

    /**
     * Create a temporary file, respecting platform differences.
     * @param prefix Must be at least 3 characters long, and does **not**
     * include "..", or "/"
     */
    abstract fun createTemporaryFile(prefix: String, suffix: String = ".tmp"): Path
}

expect fun getPlatform(): Platform