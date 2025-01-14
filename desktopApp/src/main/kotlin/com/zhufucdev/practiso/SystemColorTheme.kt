package com.zhufucdev.practiso

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.materialkolor.DynamicMaterialTheme
import com.zhufucdev.practiso.platform.JvmPlatform
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.style.primaryDark
import com.zhufucdev.practiso.style.primaryLight
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun SystemColorTheme(animate: Boolean = false, content: @Composable () -> Unit) {
    val poolingContext = remember { newSingleThreadContext("Theme") }
    DisposableEffect(true) {
        onDispose {
            poolingContext.cancel()
        }
    }
    val color by produceState(JvmPlatform.accentColor to JvmPlatform.isDarkModeEnabled) {
        withContext(poolingContext) {
            while (true) {
                delay(0.5.seconds)
                value = JvmPlatform.accentColor to JvmPlatform.isDarkModeEnabled
            }
        }
    }

    DynamicMaterialTheme(
        primary = color.first ?: if (color.second) primaryDark else primaryLight,
        useDarkTheme = color.second,
        animate = animate,
        typography = AppTypography
    ) {
        content()
    }
}

