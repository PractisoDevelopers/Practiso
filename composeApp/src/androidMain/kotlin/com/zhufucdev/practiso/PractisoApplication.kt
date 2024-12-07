package com.zhufucdev.practiso

import android.app.Application
import kotlinx.coroutines.runBlocking

class PractisoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        runBlocking { Database.migrate() }
    }

    companion object {
        lateinit var instance: PractisoApplication
    }
}