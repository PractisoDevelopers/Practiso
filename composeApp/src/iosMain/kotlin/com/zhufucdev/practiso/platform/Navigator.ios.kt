package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.MutableStateFlow

actual val Navigator: AppNavigator
    get() = UINavigator

object UINavigator : StackNavigator() {
    val path = MutableStateFlow(emptyList<AppDestination>())

    override suspend fun onNavigate(model: NavigationDestination) {
        if (model.navigation is Navigation.Home) {
            backstack.clear()
            backstack.add(AppDestination.MainView)
            pointer = 0
        }
        emitPath()
    }

    private suspend fun emitPath() {
        val p = backstack.slice(0 .. pointer)
        val start = maxOf(p.lastIndexOf(AppDestination.MainView), 0) + 1
        path.emit(p.slice(start .. pointer))
    }

    suspend fun goHome() {
        navigate(Navigation.Home)
    }

    suspend fun mutateBackstack(newValue: List<AppDestination>, pointer: Int) {
        backstack.clear()
        backstack.addAll(newValue)
        this.pointer = pointer
        emitPath()
    }
}