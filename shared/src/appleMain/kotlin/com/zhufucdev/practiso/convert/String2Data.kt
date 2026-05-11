package com.zhufucdev.practiso.convert

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding

@Suppress("CAST_NEVER_SUCCEEDS")
fun NSData(string: String): NSData? {
    return (string as NSString).dataUsingEncoding(NSUTF8StringEncoding)
}
