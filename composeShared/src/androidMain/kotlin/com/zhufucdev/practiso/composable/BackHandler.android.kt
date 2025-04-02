package com.zhufucdev.practiso.composable

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerOrIgnored(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled) { onBack() }
}