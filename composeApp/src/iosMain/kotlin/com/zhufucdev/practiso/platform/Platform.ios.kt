package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.zhufucdev.practiso.database.AppDatabase
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

object IOSPlatform : Platform() {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun createDbDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "practiso.db",
            onConfiguration = {
                it.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true))
            }
        )
    }

    override val filesystem: FileSystem
        get() = FileSystem.SYSTEM

    override val settingsFactory: Settings.Factory by lazy {
        NSUserDefaultsSettings.Factory()
    }

    override val resourcePath: Path by lazy {
        NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first()
            .toString()
            .toPath()
    }
}

actual fun getPlatform(): Platform = IOSPlatform