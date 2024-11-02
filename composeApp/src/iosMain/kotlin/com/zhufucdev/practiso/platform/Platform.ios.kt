package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.zhufucdev.practiso.database.AppDatabase
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun createDbDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "practiso.db",
            onConfiguration = {
                it.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true))
            }
        )
    }
}

private object PlatformInstance : Platform by IOSPlatform()

actual fun getPlatform(): Platform = PlatformInstance