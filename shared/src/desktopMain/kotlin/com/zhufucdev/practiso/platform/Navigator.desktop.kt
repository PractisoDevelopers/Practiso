package com.zhufucdev.practiso.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

actual val Navigator: AppNavigator
    get() = DesktopNavigator

object DesktopNavigator : StackNavigator(CoroutineScope(Dispatchers.Default))