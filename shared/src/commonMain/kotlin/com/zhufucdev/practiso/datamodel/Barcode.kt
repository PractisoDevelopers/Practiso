package com.zhufucdev.practiso.datamodel

import kotlin.math.abs
import kotlin.math.roundToInt

data class Barcode(val cornerPoints: List<PointPx>, val value: String, val type: BarcodeType)

val Barcode.center: PointPx
    get() {
        if (cornerPoints.size != 4) {
            error("Corner points do not count to 4")
        }
        return PointPx(
            x = cornerPoints.map(PointPx::x).average().roundToInt(),
            y = cornerPoints.map(PointPx::y).average().roundToInt(),
        )
    }

val Barcode.width: Int
    get() = (abs(cornerPoints[1].x - cornerPoints[0].x) + abs(cornerPoints[3].x - cornerPoints[2].x)) / 2

val Barcode.height: Int
    get() = (abs(cornerPoints[0].y - cornerPoints[2].y) + abs(cornerPoints[1].y - cornerPoints[3].y)) / 2
