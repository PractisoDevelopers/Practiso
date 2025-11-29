package com.zhufucdev.practiso.datamodel

import androidx.compose.ui.geometry.Offset

data class PointPx(val x: Int, val y: Int)

fun PointPx.toOffset() = Offset(x = x.toFloat(), y = y.toFloat())