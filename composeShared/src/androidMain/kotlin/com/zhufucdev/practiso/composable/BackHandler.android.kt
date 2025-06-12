package com.zhufucdev.practiso.composable

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
actual fun BackHandlerOrIgnored(enabled: Boolean, onBack: suspend (Flow<BackEvent>) -> Unit) {
    PredictiveBackHandler { progress ->
        onBack(progress.map {
            BackEvent(progress = it.progress)
        })
    }
}