package com.zhufucdev.practiso.platform

import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import okio.Buffer
import okio.Source
import okio.Timeout
import platform.Foundation.NSURL
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.open
import platform.posix.read as c_read

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private class AppleFileSource(private val url: NSURL) : Source {
    private val fd: Int

    init {
        url.startAccessingSecurityScopedResource()
        fd = open(url.path, O_RDONLY)
    }

    override fun close() {
        close(fd)
        url.stopAccessingSecurityScopedResource()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun read(sink: Buffer, byteCount: Long): Long =
        memScoped {
            val buf = allocArray<ByteVar>(byteCount.toInt())
            val read = c_read(fd, buf, byteCount.toULong())
            if (read > 0) {
                sink.write(buf.readBytes(read.toInt()))
                read
            } else {
                -1
            }
        }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }
}

actual suspend fun PlatformFile.source(): Source = AppleFileSource(nsUrl)