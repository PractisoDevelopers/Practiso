package com.zhufucdev.practiso

import androidx.compose.ui.graphics.Color

typealias AwtColor = java.awt.Color

fun AwtColor.toComposeColor() = Color(red = red, green = green, blue = blue, alpha = alpha)