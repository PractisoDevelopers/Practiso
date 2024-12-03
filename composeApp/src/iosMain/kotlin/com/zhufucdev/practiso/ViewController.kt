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
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel

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
        val appModel: QuizCreateViewModel =
            viewModel(factory = QuizCreateViewModel.Factory)
        val navState by UINavigator.current.collectAsState()

        LaunchedEffect(appModel) {
            appModel.loadNavOptions(navState.options)
        }

        QuizCreateApp(appModel)
    }
}

fun AnswerAppViewController(darkMode: Boolean) = ComposeUIViewController {
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
        typography = AppTypography
    ) {
        val navState by UINavigator.current.collectAsState()
        val model =
            viewModel<AnswerViewModel>(factory = AnswerViewModel.Factory)

        LaunchedEffect(model) {
            model.loadNavOptions(navState.options)
        }

        AnswerApp(model)
    }
}