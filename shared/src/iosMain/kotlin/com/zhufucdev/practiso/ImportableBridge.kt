package com.zhufucdev.practiso
import com.zhufucdev.practiso.datamodel.Importable
import okio.Buffer
import okio.ByteString.Companion.toByteString
import platform.Foundation.NSData

fun Importable(data: NSData) =
    Importable("binary data", Buffer().write(data.toByteString()))