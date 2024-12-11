package com.zhufucdev.practiso

import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.PlatformInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.use

suspend fun PlatformFile.copyTo(sink: Sink) {
    sink.buffer().use {
        it.write(readBytes())
    }
}

private class PlatformFileStreamSource(private val stream: PlatformInputStream) : Source {
    override fun close() {
        stream.close()
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        val ba = ByteArray(byteCount.toInt())
        return runBlocking(Dispatchers.IO) {
            val read = stream.readInto(ba, byteCount.toInt())
            if (read > 0) {
                sink.write(ba, 0, read)
            }
            read.toLong()
        }
    }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }
}

private class PlatformBufferSource(private val file: PlatformFile) : Source {
    private var closed = false
    override fun close() {
        closed = true
    }

    private val buffer by lazy {
        runBlocking(Dispatchers.IO) {
            file.readBytes()
        }
    }

    private var pos: Int = 0
    private val remaining get() = buffer.size - pos

    override fun read(sink: Buffer, byteCount: Long): Long {
        val rem = remaining
        if (byteCount > rem) {
            sink.write(buffer, pos, rem)
            return rem.toLong()
        } else {
            sink.write(buffer, pos, byteCount.toInt())
            return byteCount
        }
    }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }
}

fun PlatformFile.source(): Source =
    if (supportsStreams()) {
        PlatformFileStreamSource(getStream())
    } else {
        PlatformBufferSource(this)
    }