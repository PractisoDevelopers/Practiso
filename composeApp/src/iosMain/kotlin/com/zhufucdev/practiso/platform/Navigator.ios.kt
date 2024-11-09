package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.MutableStateFlow

actual val Navigator: AppNavigator
    get() = UINavigator

object UINavigator : StackNavigator() {
    val path = MutableStateFlow(emptyList<NavigatorStackItem>())

    override suspend fun onNavigate(model: NavigationStateSnapshot) {
        if (model.navigation is Navigation.Home) {
            backstack.clear()
            backstack.add(NavigatorStackItem(AppDestination.MainView, model.options))
            pointer = 0
        }
        emitPath()
    }

    private suspend fun emitPath() {
        val p = backstack.slice(0 .. pointer)
        val start = maxOf(p.indexOfLast { it.destination == AppDestination.MainView }, 0) + 1
        path.emit(p.slice(start .. pointer))
    }

    suspend fun goHome() {
        navigate(Navigation.Home)
    }

    suspend fun mutateBackstack(newValue: List<NavigatorStackItem>, pointer: Int) {
        backstack.clear()
        backstack.addAll(newValue)
        this.pointer = pointer
        emitPath()
    }
}