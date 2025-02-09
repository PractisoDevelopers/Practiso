package com.zhufucdev.practiso
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.platform.AppleFileSource
import okio.Buffer
import okio.ByteString.Companion.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSURL

fun Importable(data: NSData) =
    Importable("binary data", Buffer().write(data.toByteString()))

fun Importable(url: NSURL) =
    Importable(url.lastPathComponent ?: "generic file", AppleFileSource(url))
