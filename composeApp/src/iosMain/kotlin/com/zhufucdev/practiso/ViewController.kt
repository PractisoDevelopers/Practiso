package com.zhufucdev.practiso

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.window.ComposeUIViewController
import com.zhufucdev.practiso.style.AppTypography

fun MainViewController(darkMode: Boolean) = ComposeUIViewController {
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
        typography = AppTypography
    ) {
        PractisoApp()
    }
}

fun QuizCreateViewController(darkMode: Boolean) = ComposeUIViewController {
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
        typography = AppTypography
    ) {
        QuizCreateApp()
    }
}