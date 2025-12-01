package com.zhufucdev.practiso.helper

import androidx.compose.ui.geometry.Offset
import com.zhufucdev.practiso.datamodel.PointPx

fun PointPx.toOffset() = Offset(x = x.toFloat(), y = y.toFloat())
