package com.zhufucdev.practiso.platform

actual fun isDebugBuild(): Boolean {
    return System.getenv("DEBUG") == "1"
}