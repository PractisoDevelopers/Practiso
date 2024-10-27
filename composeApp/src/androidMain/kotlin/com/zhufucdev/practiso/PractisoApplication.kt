package com.zhufucdev.practiso

import android.app.Application

class PractisoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PractisoApplication
    }
}