package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.PointPx
import com.zhufucdev.practiso.datamodel.getBarcodeType
import com.google.mlkit.vision.barcode.common.Barcode as MlKitBarcode

fun MlKitBarcode.toBarcode(): Barcode {
    val value = rawValue ?: error("ML Kit barcode has null raw value")
    return Barcode(
        cornerPoints = cornerPoints?.map { PointPx(it.x, it.y) } ?: emptyList(),
        value = value,
        type = value.getBarcodeType()
    )
}

