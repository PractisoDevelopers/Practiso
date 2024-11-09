package com.zhufucdev.practiso

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.platform.UINavigator
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import com.zhufucdev.practiso.viewmodel.QuizViewModel

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
        val quizModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory)
        val appModel: QuizCreateViewModel =
            viewModel(factory = QuizCreateViewModel.Factory)
        val navState by UINavigator.current.collectAsState()

        LaunchedEffect(quizModel, appModel, navState) {
            quizModel.frames.clear()
            QuizCreateApp.manipulateViewModelsWithNavigationOptions(
                appModel,
                quizModel,
                navState.options
            )
        }

        QuizCreateApp(appModel, quizModel)
    }
}