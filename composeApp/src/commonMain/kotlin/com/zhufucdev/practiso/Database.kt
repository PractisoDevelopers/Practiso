package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase

object Database {
    val app: AppDatabase by lazy {
        getPlatform().createDbDriver().toDatabase()
    }
}