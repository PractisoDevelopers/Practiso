package com.zhufucdev.practiso.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.typeOf

actual val Navigator: AppNavigator
    get() = DesktopNavigator

object DesktopNavigator : StackNavigator(CoroutineScope(Dispatchers.Default)) {
    inline fun <reified T> setResult(value: T) {
        val entry = backstack.last()
        assert(entry.navigation.destination.resultType == typeOf<T>())
        @Suppress("UNCHECKED_CAST")
        (entry as BackstackEntry<T>).result = StackNavigatorResult.Ok(value)
    }
}