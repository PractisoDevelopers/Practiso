package com.zhufucdev.practiso

import android.content.Context

val SharedContext: Context by lazy {
    PractisoApp.instance
        ?: PractisoApp::class.java.classLoader
            ?.loadClass("androidx.test.platform.app.InstrumentationRegistry")
            ?.getMethod("getInstrumentation")
            ?.invoke(null)
            ?.let { it::class.java.getMethod("getContext").invoke(it) }
                as Context?
        ?: throw IllegalStateException("No shared context available.")
}