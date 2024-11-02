package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.singleWindowApplication
import com.zhufucdev.practiso.page.QuizCreateApp
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.DesktopNavigator
import com.zhufucdev.practiso.platform.PlatformInstance
import com.zhufucdev.practiso.style.AppTypography
import com.zhufucdev.practiso.style.darkScheme
import com.zhufucdev.practiso.style.lightScheme

fun main() = singleWindowApplication(title = "Practiso") {
    val destination by DesktopNavigator.current.collectAsState()
    MaterialTheme(
        colorScheme = if (PlatformInstance.isDarkModeEnabled) darkScheme else lightScheme,
        typography = AppTypography
    ) {
        Surface {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                        .togetherWith(fadeOut())
                }
            ) { dest ->
                when (dest) {
                    AppDestination.MainView -> App()
                    AppDestination.QuizCreate -> QuizCreateApp()
                }
            }
        }
    }
}

