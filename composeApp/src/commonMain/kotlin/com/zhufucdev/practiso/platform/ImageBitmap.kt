package com.zhufucdev.practiso.platform

import androidx.compose.ui.graphics.ImageBitmap

interface ImageBitmapLoader {
    fun from(ba: ByteArray): ImageBitmap
}

expect val BitmapLoader: ImageBitmapLoader