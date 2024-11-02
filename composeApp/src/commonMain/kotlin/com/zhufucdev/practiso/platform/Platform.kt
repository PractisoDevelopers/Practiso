package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver

interface Platform {
    val name: String
    fun createDbDriver(): SqlDriver
}

expect fun getPlatform(): Platform