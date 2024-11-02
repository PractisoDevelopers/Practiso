package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual val Navigator: AppNavigator
    get() = DesktopNavigator

object DesktopNavigator : AppNavigator {
    private val _destination = MutableStateFlow(AppDestination.MainView)
    override val current: StateFlow<AppDestination> = _destination.asStateFlow()

    override suspend fun navigate(destination: AppDestination) {
        _destination.emit(destination)
    }
}