package com.zhufucdev.practiso.platform

actual val Navigator: AppNavigator
    get() = DesktopNavigator

object DesktopNavigator : StackNavigator()