package com.zhufucdev.practiso

import okio.BufferedSink
import okio.BufferedSource

fun BufferedSink.writeDouble(value: Double) {
    writeLong(value.toRawBits())
}

fun BufferedSource.readDouble(): Double = Double.fromBits(readLong())