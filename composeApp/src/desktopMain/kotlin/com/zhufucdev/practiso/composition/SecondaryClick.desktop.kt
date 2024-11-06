package com.zhufucdev.practiso.composition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.combineClickable(onClick: (() -> Unit)?, onSecondaryClick: (() -> Unit)?) =
    if (onClick != null) {
        clickable(onClick = onClick)
    } else {
        this
    } then if (onSecondaryClick == null) {
        Modifier
    } else {
        onClick(
            matcher = PointerMatcher.mouse(PointerButton.Secondary),
            onClick = onSecondaryClick
        )
    }
