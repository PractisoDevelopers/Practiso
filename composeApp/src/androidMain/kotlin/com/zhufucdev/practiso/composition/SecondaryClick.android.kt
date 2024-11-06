package com.zhufucdev.practiso.composition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.combineClickable(
    onClick: (() -> Unit)?,
    onSecondaryClick: (() -> Unit)?,
): Modifier =
    if (onClick != null) {
        combinedClickable(onClick = onClick, onLongClick = onSecondaryClick)
    } else if (onSecondaryClick != null) {
        pointerInput(true) {
            detectTapGestures(onLongPress = { onSecondaryClick() })
        }
    } else {
        this
    }