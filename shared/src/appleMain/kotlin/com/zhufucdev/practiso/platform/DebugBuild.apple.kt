package com.zhufucdev.practiso.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean {
    return Platform.isDebugBinary
}