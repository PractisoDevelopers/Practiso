package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.composable.SystemColorTheme
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.DesktopNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationStateSnapshot
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import okio.source
import java.awt.Desktop
import java.io.File

fun main(args: Array<String>) {
    val openFileChannel = Channel<List<File>>(capacity = 1)
    if (Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)
    ) {
        Desktop.getDesktop().setOpenFileHandler { event ->
            openFileChannel.trySend(event.files)
        }
    }

    if (args.isNotEmpty()) {
        openFileChannel.trySend(args.map(::File))
    }

    singleWindowApplication(title = "Practiso") {
        val navState by DesktopNavigator.current.collectAsState()
        val navController = rememberNavController()
        val importer: ImportViewModel = viewModel(factory = ImportViewModel.Factory)

        DisposableEffect(true) {
            onDispose {
                DesktopNavigator.coroutineScope.cancel()
            }
        }

        LaunchedEffect(importer) {
            while (true) {
                select {
                    openFileChannel.onReceive { file ->
                        file.forEach { file ->
                            file.inputStream().use { ips ->
                                val target = Importable(
                                    name = file.name,
                                    source = ips.source()
                                )

                                importer.import(target)
                            }
                        }
                    }
                }
            }
        }

        SystemColorTheme(animate = true) {
            Surface {
                AnimatedContent(
                    targetState = navState,
                    transitionSpec = mainFrameTransitionSpec
                ) { state ->
                    when (state.destination) {
                        AppDestination.MainView -> PractisoApp(
                            navController,
                            importViewModel = importer
                        )

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