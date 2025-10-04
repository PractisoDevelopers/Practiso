package com.zhufucdev.practiso.platform

import platform.UIKit.UIDevice

object IOSPlatform : ApplePlatform() {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override val deviceName: String
        get() = UIDevice.currentDevice.name
}

actual fun getPlatform(): Platform = IOSPlatform