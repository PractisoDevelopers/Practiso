package com.zhufucdev.practiso.composition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.secondaryClickable(onClick: () -> Unit) = composed {
    onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary), onClick = onClick)
}