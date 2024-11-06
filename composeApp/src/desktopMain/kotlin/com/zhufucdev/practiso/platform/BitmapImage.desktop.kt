package com.zhufucdev.practiso.platform

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual val BitmapLoader: ImageBitmapLoader = object : ImageBitmapLoader {
    override fun from(ba: ByteArray) = Image.makeFromEncoded(ba).toComposeImageBitmap()
}