package com.zhufucdev.practiso.platform

import kotlinx.cinterop.ExperimentalForeignApi

interface Vibrator {
    fun wobble()
}

var sharedVibrator: Vibrator? = null

@OptIn(ExperimentalForeignApi::class)
actual fun wobbleHapticFeedback() {
    sharedVibrator?.wobble()
}