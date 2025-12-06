package com.zhufucdev.practiso.helper

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.IntFlagSet
import com.zhufucdev.practiso.datamodel.PointPx
import com.zhufucdev.practiso.datamodel.getBarcodeType
import kotlin.math.roundToInt

fun IntFlagSet<BarcodeType>.toBarcodeFormats() = buildList {
    if (this@toBarcodeFormats.contains(BarcodeType.AUTHORIZATION_TOKEN)) {
        add(BarcodeFormat.QR_CODE)
    }
}

fun ResultPoint.toPointPx() = PointPx(
    x = x.roundToInt(),
    y = y.roundToInt()
)

fun Array<Result>.toBarcodes() = map {
    Barcode(
        cornerPoints = it.resultPoints.map(ResultPoint::toPointPx),
        value = it.text,
        type = it.text.getBarcodeType()
    )
}
