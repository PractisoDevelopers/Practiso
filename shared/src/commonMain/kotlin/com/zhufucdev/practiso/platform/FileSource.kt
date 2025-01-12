package com.zhufucdev.practiso.platform

import io.github.vinceglb.filekit.core.PlatformFile
import io.github.vinceglb.filekit.core.PlatformInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Source
import okio.Timeout

 class PlatformFileStreamSource(private val stream: PlatformInputStream) : Source {
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

expect suspend fun PlatformFile.source(): Source
