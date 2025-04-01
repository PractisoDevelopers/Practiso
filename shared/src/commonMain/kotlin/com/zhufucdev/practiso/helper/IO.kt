package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.platform.Platform
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import okio.Sink
import okio.buffer
import okio.use

suspend fun PlatformFile.copyTo(sink: Sink) {
    sink.buffer().use {
        it.write(readBytes())
    }
}

fun Platform.resourceSink(name: String) = filesystem.sink(resourcePath.resolve(name))
