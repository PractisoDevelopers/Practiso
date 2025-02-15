package com.zhufucdev.practiso.bridge

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import okio.Source
import okio.buffer
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun Data(source: Source) = memScoped {
    val ba = source.buffer().readByteArray()
    NSData.create(bytes = allocArrayOf(ba), length = ba.size.toULong())
}
