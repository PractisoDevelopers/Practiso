package com.zhufucdev.practiso.platform

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.zhufucdev.practiso.convert.toPath
import com.zhufucdev.practiso.database.AppDatabase
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.temporaryDirectory

abstract class ApplePlatform : Platform() {
    override fun createDbDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AppDatabase.Schema.synchronous(),
            name = "practiso.db",
            onConfiguration = {
                it.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true))
            }
        )
    }

    override val filesystem: FileSystem
        get() = FileSystem.SYSTEM

    override val resourcePath: Path by lazy {
        NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first()
            .toString()
            .toPath()
    }

    override val logicalProcessorsCount: Int
        get() = NSProcessInfo().activeProcessorCount.toInt()

    override fun createTemporaryFile(prefix: String, suffix: String): Path {
        if (prefix.contains("..") || prefix.contains("/")) {
            throw IllegalArgumentException("Path penetration in prefix.")
        }
        if (prefix.length < 3) {
            throw IllegalArgumentException("Prefix is shorter than 3 characters.")
        }
        val randomName = randomUUID()
        val safePrefix = if (prefix.isNotEmpty()) "$prefix-" else prefix
        return NSFileManager.defaultManager.temporaryDirectory.toPath().resolve(
            "$safePrefix-$randomName$suffix"
        )
    }
}
