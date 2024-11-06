package com.zhufucdev.practiso.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

actual val BitmapLoader: ImageBitmapLoader = object : ImageBitmapLoader {
    override fun from(ba: ByteArray) = BitmapFactory.decodeByteArray(ba, 0, ba.size).asImageBitmap()
}