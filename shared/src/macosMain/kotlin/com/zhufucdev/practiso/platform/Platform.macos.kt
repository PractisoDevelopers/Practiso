package com.zhufucdev.practiso.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSHost
import platform.Foundation.NSProcessInfo

object MacOsPlatform : ApplePlatform() {
    @OptIn(ExperimentalForeignApi::class)
    override val name: String
        get() = NSProcessInfo.processInfo.operatingSystemVersion.useContents {
            "macOS $majorVersion.$minorVersion"
        }
    override val deviceName: String
        get() = NSHost.currentHost().let { it.localizedName ?: it.name ?: "Mac" }
}

actual fun getPlatform(): Platform = MacOsPlatform