package com.zhufucdev.practiso.platform

import io.github.vinceglb.filekit.core.PlatformFile
import okio.Buffer
import okio.Source

actual suspend fun PlatformFile.source(): Source =
    if (supportsStreams()) {
        PlatformFileStreamSource(getStream())
    } else {
        Buffer().apply { write(readBytes()) }
    }