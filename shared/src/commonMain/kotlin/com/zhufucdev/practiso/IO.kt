package com.zhufucdev.practiso

import io.github.vinceglb.filekit.core.PlatformFile
import okio.Sink
import okio.buffer
import okio.use

suspend fun PlatformFile.copyTo(sink: Sink) {
    sink.buffer().use {
        it.write(readBytes())
    }
}
