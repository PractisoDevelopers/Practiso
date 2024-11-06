package com.zhufucdev.practiso

import io.github.vinceglb.filekit.core.PlatformFile
import okio.Sink
import okio.buffer
import okio.use

suspend fun Sink.copyFrom(file: PlatformFile) {
    buffer().use {
        it.write(file.readBytes())
    }
}