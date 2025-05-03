package com.zhufucdev.practiso

import app.cash.sqldelight.db.SqlDriver
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.helper.toDatabase
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.FeiService

object Database {
    val driver: SqlDriver by lazy {
        getPlatform().createDbDriver()
    }

    val app: AppDatabase by lazy {
        driver.toDatabase()
    }

    val fei: FeiService by lazy {
        FeiService(parallelTasks = getPlatform().logicalProcessorsCount)
    }
}
