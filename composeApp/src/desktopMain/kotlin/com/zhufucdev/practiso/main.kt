package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.singleWindowApplication
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.DesktopNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.PlatformInstance
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.style.darkScheme
import com.zhufucdev.practiso.style.lightScheme

fun main() = singleWindowApplication(title = "Practiso") {
    val destination by DesktopNavigator.current.collectAsState()
    val navController = rememberNavController()

    MaterialTheme(
        colorScheme = if (PlatformInstance.isDarkModeEnabled) darkScheme else lightScheme,
        typography = AppTypography
    ) {
        Surface {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    if (destination.navigation is Navigation.Forward || destination.navigation is Navigation.Goto) {
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
            ) { model ->
                when (model.destination) {
                    AppDestination.MainView -> PractisoApp(navController)
                    AppDestination.QuizCreate -> QuizCreateApp()
                }
            }
        }
    }
}

