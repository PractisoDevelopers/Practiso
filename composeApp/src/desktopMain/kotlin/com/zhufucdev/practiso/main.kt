package com.zhufucdev.practiso

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.singleWindowApplication
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.style.darkScheme
import com.zhufucdev.practiso.style.lightScheme

fun main() {
    singleWindowApplication {
        MaterialTheme(
            colorScheme = if (PlatformInstance.isDarkModeEnabled) darkScheme else lightScheme,
            typography = AppTypography
        ) {
            App()
        }
    }
}

