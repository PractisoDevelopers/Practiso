package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.DesktopNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationStateSnapshot
import com.zhufucdev.practiso.platform.PlatformInstance
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.style.darkScheme
import com.zhufucdev.practiso.style.lightScheme
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        Database.migrate()
    }

    singleWindowApplication(title = "Practiso") {
        val navState by DesktopNavigator.current.collectAsState()
        val navController = rememberNavController()

        DisposableEffect(true) {
            onDispose {
                DesktopNavigator.coroutineScope.cancel()
            }
        }

        MaterialTheme(
            colorScheme = if (PlatformInstance.isDarkModeEnabled) darkScheme else lightScheme,
            typography = AppTypography
        ) {
            Surface {
                AnimatedContent(
                    targetState = navState,
                    transitionSpec = mainFrameTransitionSpec
                ) { state ->
                    when (state.destination) {
                        AppDestination.MainView -> PractisoApp(navController)
                        AppDestination.QuizCreate -> {
                            val appModel: QuizCreateViewModel =
                                viewModel(factory = QuizCreateViewModel.Factory)

                            LaunchedEffect(appModel) {
                                appModel.loadNavOptions(navState.options)
                            }

                            QuizCreateApp(appModel)
                        }

                        AppDestination.Answer -> {
                            val model =
                                viewModel<AnswerViewModel>(factory = AnswerViewModel.Factory)

                            LaunchedEffect(model, navState) {
                                model.loadNavOptions(navState.options)
                            }

                            AnswerApp(model)
                        }
                    }
                }
            }
        }
    }

}

val mainFrameTransitionSpec: AnimatedContentTransitionScope<NavigationStateSnapshot>.() -> ContentTransform =
    {
        if (targetState.navigation is Navigation.Forward || targetState.navigation is Navigation.Goto) {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
                .togetherWith(fadeOut())
        } else {
            fadeIn()
                .togetherWith(
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                )
        }
    }