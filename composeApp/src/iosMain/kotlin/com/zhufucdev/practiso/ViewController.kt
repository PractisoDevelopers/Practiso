package com.zhufucdev.practiso

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.style.AppTypography

fun MainViewController(darkMode: Boolean) = ComposeUIViewController {
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
        typography = AppTypography
    ) {
        PractisoApp(
            navController = rememberNavController()
        )
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