package com.zhufucdev.practiso.bridge

import com.zhufucdev.practiso.datamodel.NamedSource
import io.github.vinceglb.filekit.PlatformFile
import okio.Buffer
import okio.ByteString.Companion.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSURL

fun NamedSource(data: NSData) =
    NamedSource("binary data", Buffer().write(data.toByteString()))

fun NamedSource(url: NSURL) = NamedSource.fromFile(PlatformFile(url))
