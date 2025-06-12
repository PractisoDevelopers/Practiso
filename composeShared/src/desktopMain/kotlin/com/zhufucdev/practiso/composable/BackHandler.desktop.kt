package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
actual fun BackHandlerOrIgnored(enabled: Boolean, onBack: suspend (Flow<BackEvent>) -> Unit) {
}