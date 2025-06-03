package com.zhufucdev.practiso.helper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

fun Color.inverted(): Color =
    convert(ColorSpaces.CieLab).let { it.copy(red = 100 - it.red) }.convert(colorSpace)