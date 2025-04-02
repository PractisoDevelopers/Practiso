package com.zhufucdev.practiso.platform

import platform.UIKit.UIDevice

object IOSPlatform : ApplePlatform() {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform