package com.zhufucdev.practiso

import android.app.Application

abstract class PractisoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this
    }

    companion object {
        var instance: PractisoApp? = null
    }
}