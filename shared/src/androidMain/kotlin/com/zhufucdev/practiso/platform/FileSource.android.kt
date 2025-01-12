package com.zhufucdev.practiso.platform

import io.github.vinceglb.filekit.core.PlatformFile
import okio.Source

actual suspend fun PlatformFile.source(): Source = PlatformFileStreamSource(getStream())