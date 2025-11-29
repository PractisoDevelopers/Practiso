package com.zhufucdev.practiso.datamodel

import com.google.mlkit.vision.barcode.common.Barcode as MlKitBarcode

data class Barcode(val cornerPoints: List<PointPx>, val value: String)

fun MlKitBarcode.toBarcode(): Barcode = Barcode(
    cornerPoints = cornerPoints?.map { PointPx(it.x, it.y) } ?: emptyList(),
    value = rawValue ?: error("ML Kit barcode has null raw value"),
)