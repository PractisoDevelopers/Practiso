package com.zhufucdev.practiso

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.zhufucdev.practiso.ui.AppTypography
import com.zhufucdev.practiso.ui.darkScheme
import com.zhufucdev.practiso.ui.lightScheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Practiso",
    ) {
        MaterialTheme(
            colorScheme = if (PlatformInstance.isDarkModeEnabled) darkScheme else lightScheme,
            typography = AppTypography
        ) {
            App()
        }
    }
}