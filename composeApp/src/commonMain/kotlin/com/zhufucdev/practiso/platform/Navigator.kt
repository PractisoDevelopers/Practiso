package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.StateFlow

enum class AppDestination {
    MainView,
    QuizCreate
}

interface AppNavigator {
    val current: StateFlow<AppDestination>
    suspend fun navigate(destination: AppDestination)
}

expect val Navigator: AppNavigator