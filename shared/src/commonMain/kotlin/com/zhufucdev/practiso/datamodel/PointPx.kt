package com.zhufucdev.practiso.datamodel

import kotlin.math.roundToInt

data class PointPx(val x: Int, val y: Int)

operator fun PointPx.div(factor: Float) =
    PointPx((x / factor).roundToInt(), (y / factor).roundToInt())

operator fun PointPx.times(factor: Float) =
    PointPx((x * factor).roundToInt(), (y * factor).roundToInt())