package com.zhufucdev.practiso

import com.zhufucdev.practiso.platform.getPlatform
import platform.Foundation.NSURL

object ResourceService {
    fun resolve(fileName: String) =
        NSURL(fileURLWithPath = getPlatform().resourcePath.resolve(fileName).normalized().toString())
}
