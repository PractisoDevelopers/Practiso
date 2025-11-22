package com.zhufucdev.practiso.platform

import android.content.pm.ApplicationInfo
import com.zhufucdev.practiso.SharedContext

actual fun isDebugBuild(): Boolean {
    return try {
        SharedContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (_: IllegalStateException) {
        false
    }
}