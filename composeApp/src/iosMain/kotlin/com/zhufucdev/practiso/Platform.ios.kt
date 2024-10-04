package com.zhufucdev.practiso

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.zhufucdev.practiso.database.AppDatabase
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override suspend fun createDbDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "practiso.db")
    }
}

private object PlatformInstance: Platform by IOSPlatform()

actual fun getPlatform(): Platform = PlatformInstance