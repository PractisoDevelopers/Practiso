package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.platform.Platform
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.io.okio.asOkioSource
import okio.Sink
import okio.Source
import okio.buffer
import usearch.Index

fun PlatformFile.copyTo(sink: Sink) {
    this.source().asOkioSource().buffer().readAll(sink)
}

fun Platform.resourceSink(name: String) = filesystem.sink(resourcePath.resolve(name))

fun Index.saveTo(sink: Sink) {
    val ba = ByteArray(serializedLength.toInt())
    saveBuffer(ba)
    sink.buffer().write(ba).flush()
}

fun Index.readFrom(source: Source) {
    val ba = source.buffer().readByteArray()
    loadBuffer(ba)
}