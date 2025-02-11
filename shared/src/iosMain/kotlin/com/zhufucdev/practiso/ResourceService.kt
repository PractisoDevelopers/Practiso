package com.zhufucdev.practiso

import com.zhufucdev.practiso.platform.IOSPlatform
import platform.Foundation.NSURL

object ResourceService {
    fun resolve(fileName: String) =
        NSURL(fileURLWithPath = IOSPlatform.resourcePath.resolve(fileName).normalized().toString())
}