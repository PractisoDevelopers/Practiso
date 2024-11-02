package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual val Navigator: AppNavigator
    get() = UINavigator

object UINavigator : AppNavigator {
    private val _destination = MutableStateFlow(AppDestination.MainView)
    override val current: StateFlow<AppDestination> = _destination.asStateFlow()

    override suspend fun navigate(destination: AppDestination) {
        this._destination.emit(destination)
    }
}