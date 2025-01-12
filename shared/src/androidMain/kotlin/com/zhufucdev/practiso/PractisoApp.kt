package com.zhufucdev.practiso

import android.app.Activity
import android.app.Application
import com.zhufucdev.practiso.platform.AppDestination

abstract class PractisoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    abstract fun getActivity(destination: AppDestination): Class<out Activity>

    companion object {
        lateinit var instance: PractisoApp
    }
}