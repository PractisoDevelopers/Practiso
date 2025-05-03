package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.platform.Platform
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import okio.Sink
import okio.Source
import okio.buffer
import okio.use
import usearch.Index

suspend fun PlatformFile.copyTo(sink: Sink) {
    sink.buffer().use {
        it.write(readBytes())
    }
}

fun Platform.resourceSink(name: String) = filesystem.sink(resourcePath.resolve(name))

fun Index.saveTo(sink: Sink) {
    val ba = ByteArray(memoryUsage.toInt())
    saveBuffer(ba)
    sink.buffer().write(ba)
}

fun Index.readFrom(source: Source) {
    val ba = source.buffer().readByteArray()
    loadBuffer(ba)
}