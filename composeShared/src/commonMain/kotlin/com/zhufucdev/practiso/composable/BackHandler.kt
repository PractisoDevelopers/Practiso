package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

data class BackEvent(val progress: Float)

@Composable
expect fun BackHandlerOrIgnored(enabled: Boolean = true, onBack: suspend (Flow<BackEvent>) -> Unit)
