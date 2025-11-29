package com.zhufucdev.practiso.composable

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.toOffset
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import opacity.client.OpacityClient
import opacity.client.Whoami

@Composable
fun BarcodeOverlay(
    modifier: Modifier = Modifier,
    barcodes: List<Barcode>
) {
    Canvas(modifier) {
        barcodes.forEach {
            it.cornerPoints.forEach { point ->
                drawCircle(Color.Red, radius = 12f, center = point.toOffset())
            }
        }
    }
}

interface BarcodeOverlayState {
    suspend fun getWhoamiResult(authToken: String): Result<Whoami>
}

fun interface CoordinationTransformer {
    fun transform(value: PointPx): Offset
}