package com.zhufucdev.practiso.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

enum class AppDestination {
    MainView,
    QuizCreate,
    Answer
}

@Serializable
sealed interface NavigationOption {
    @Serializable
    data class OpenQuiz(val quizId: Long) : NavigationOption

    @Serializable
    data class OpenTake(val takeId: Long) : NavigationOption
}

sealed interface Navigation {
    interface WithDestination : Navigation {
        val destination: AppDestination
    }

    data class Goto(
        override val destination: AppDestination,
    ) : WithDestination

    data object Forward : Navigation
    data object Backward : Navigation
    data object Home : WithDestination {
        override val destination get() = AppDestination.MainView
    }
}

data class NavigationStateSnapshot(
    val navigation: Navigation,
    val destination: AppDestination,
    val options: List<NavigationOption> = emptyList(),
)

interface AppNavigator {
    val current: StateFlow<NavigationStateSnapshot>
    suspend fun navigate(navigation: Navigation, options: List<NavigationOption> = emptyList())
}

data class NavigatorStackItem(
    val destination: AppDestination,
    val options: List<NavigationOption>,
)

abstract class StackNavigator(val coroutineScope: CoroutineScope) : AppNavigator {
    protected val state =
        MutableStateFlow(NavigationStateSnapshot(Navigation.Home, Navigation.Home.destination))

    override val current: StateFlow<NavigationStateSnapshot> = state.asStateFlow()

    protected val backstack =
        mutableListOf(NavigatorStackItem(Navigation.Home.destination, emptyList()))
    protected var pointer = 0

    private val stateChannel = Channel<NavigationStateSnapshot>()

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    stateChannel.onReceive {
                        state.emit(it)
                        onNavigate(state.value)
                    }
                }
            }
        }
    }

    override suspend fun navigate(navigation: Navigation, options: List<NavigationOption>) {
        when (navigation) {
            is Navigation.Backward -> {
                if (pointer <= 0) {
                    error("Backstack will become empty")
                }
                val dest = backstack[--pointer]
                stateChannel.send(
                    NavigationStateSnapshot(
                        navigation,
                        dest.destination,
                        dest.options + options
                    )
                )
            }

            is Navigation.Forward -> {
                if (pointer >= backstack.lastIndex) {
                    error("Backstack is currently at the edge")
                }
                val dest = backstack[++pointer]
                stateChannel.send(
                    NavigationStateSnapshot(
                        navigation,
                        dest.destination,
                        dest.options + options
                    )
                )
            }

            is Navigation.WithDestination -> {
                if (pointer < backstack.lastIndex) {
                    repeat(pointer - backstack.lastIndex) {
                        backstack.removeAt(pointer)
                    }
                }
                backstack.add(NavigatorStackItem(navigation.destination, options))
                pointer++
                stateChannel.send(NavigationStateSnapshot(navigation, navigation.destination, options))
            }
        }
    }

    open suspend fun onNavigate(model: NavigationStateSnapshot) {
    }
}

expect val Navigator: AppNavigator