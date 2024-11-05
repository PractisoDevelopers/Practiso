package com.zhufucdev.practiso.composition

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

expect fun Modifier.secondaryClickable(onClick: () -> Unit): Modifier

internal fun Modifier.longClickable(onClick: () -> Unit) = composed {
    val haptics = LocalHapticFeedback.current
    pointerInput(true) {
        detectTapGestures(onLongPress = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        })
    }
}