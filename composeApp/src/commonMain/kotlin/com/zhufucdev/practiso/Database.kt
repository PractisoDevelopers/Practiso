package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.platform.getPlatform

object Database {
    val app: AppDatabase by lazy {
        getPlatform().createDbDriver().toDatabase()
    }
}