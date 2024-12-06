package com.zhufucdev.practiso

import android.app.Application
import kotlinx.coroutines.runBlocking

class PractisoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runBlocking { Database.migrate() }
        instance = this
    }

    companion object {
        lateinit var instance: PractisoApplication
    }
}