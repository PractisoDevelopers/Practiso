package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppDestination {
    MainView,
    QuizCreate
}


sealed interface Navigation {
    interface WithDestination : Navigation {
        val destination: AppDestination
    }

    data class Goto(override val destination: AppDestination) : WithDestination
    data object Forward : Navigation
    data object Backward : Navigation
    data object Home : WithDestination {
        override val destination get() = AppDestination.MainView
    }
}

data class NavigationDestination(val navigation: Navigation, val destination: AppDestination)

infix fun Navigation.and(destination: AppDestination) = NavigationDestination(this, destination)


interface AppNavigator {
    val current: StateFlow<NavigationDestination>
    suspend fun navigate(navigation: Navigation)
}

abstract class StackNavigator : AppNavigator {
    protected val _navigation =
        MutableStateFlow(Navigation.Home and Navigation.Home.destination)

    override val current: StateFlow<NavigationDestination> = _navigation.asStateFlow()

    protected val backstack = mutableListOf(Navigation.Home.destination)
    protected var pointer = 0

    override suspend fun navigate(navigation: Navigation) {
        when (navigation) {
            is Navigation.Backward -> {
                if (pointer <= 0) {
                    error("Backstack will become empty")
                }
                val dest = backstack[--pointer]
                _navigation.emit(navigation and dest)
            }

            is Navigation.Forward -> {
                if (pointer >= backstack.lastIndex) {
                    error("Backstack is currently at the edge")
                }
                val dest = backstack[++pointer]
                _navigation.emit(navigation and dest)
            }

            is Navigation.WithDestination -> {
                if (pointer < backstack.lastIndex) {
                    repeat(pointer - backstack.lastIndex) {
                        backstack.removeAt(pointer)
                    }
                }
                backstack.add(navigation.destination)
                pointer++
                _navigation.emit(navigation and navigation.destination)
            }
        }
        onNavigate(_navigation.value)
    }

    open suspend fun onNavigate(model: NavigationDestination) {
    }
}

expect val Navigator: AppNavigator