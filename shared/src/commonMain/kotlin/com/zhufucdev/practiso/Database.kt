package com.zhufucdev.practiso

import app.cash.sqldelight.db.SqlDriver
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.helper.toDatabase
import com.zhufucdev.practiso.platform.getPlatform

object Database {
    val driver: SqlDriver by lazy {
        getPlatform().createDbDriver()
    }

    val app: AppDatabase by lazy {
        driver.toDatabase()
    }

    val vectorDriver by lazy {
        getPlatform().createVectorDbDriver()
    }
}
