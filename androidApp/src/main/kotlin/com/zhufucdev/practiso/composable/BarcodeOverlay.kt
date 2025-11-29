package com.zhufucdev.practiso.composable

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.PointPx

@Composable
fun BarcodeOverlay(
    modifier: Modifier = Modifier,
    coordinationTransformer: CoordinationTransformer,
    barcodes: List<Barcode>
) {
    val barcodes = remember(barcodes) {
        barcodes.map {
            DisplayBarcode(
                cornerPoints = it.cornerPoints.map(coordinationTransformer::transform),
                barcode = it
            )
        }
    }

    Canvas(modifier) {
        barcodes.forEach {
            it.cornerPoints.forEach { point ->
                drawCircle(Color.Red, radius = 12f, center = point)
            }
        }
    }
}

private data class DisplayBarcode(
    val cornerPoints: List<Offset>,
    val barcode: Barcode
)

fun interface CoordinationTransformer {
    fun transform(value: PointPx): Offset
}