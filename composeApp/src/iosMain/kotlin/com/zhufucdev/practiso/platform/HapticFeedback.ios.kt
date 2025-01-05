package com.zhufucdev.practiso.platform

import kotlinx.cinterop.ExperimentalForeignApi

data class Vibrator(
    val wobble: () -> Unit
)

var sharedVibrator: Vibrator? = null

@OptIn(ExperimentalForeignApi::class)
actual fun wobbleHapticFeedback() {
    sharedVibrator?.wobble?.invoke()
}