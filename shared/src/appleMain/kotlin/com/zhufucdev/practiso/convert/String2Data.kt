package com.zhufucdev.practiso.convert

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.cstr
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
fun MemScope.NSData(string: String): NSData {
    return NSData.dataWithBytes(
        string.cstr.ptr,
        length = string.length.toULong() + 1u/* with trailing zero terminator */
    )
}
