package com.zhufucdev.practiso

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.style.PaddingSpace
import org.jetbrains.compose.resources.decodeToImageBitmap
import qrcode.QRCode

@Composable
fun QrCodeViewerApp(model: QRCode, title: String?) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { title?.let { Text(it) } },
            navigationIcon = { NavigateUpButton() }
        )
    }) { innerPadding ->
        var displayModel by remember { mutableStateOf(model) }
        Layout(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingSpace)
                .fillMaxSize()
                .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp),
            content = {
                Image(qrCode = model, contentDescription = null)
            }
        ) { measurables, constraints ->
            val shorterEdge = minOf(constraints.maxHeight, constraints.maxWidth)
            if (displayModel.canvasSize != shorterEdge) {
                displayModel = displayModel.resize(shorterEdge)
            }
            val childConstraints = Constraints(
                maxWidth = shorterEdge,
                maxHeight = shorterEdge,
                minWidth = shorterEdge,
                minHeight = shorterEdge
            )
            val placeables = measurables.map { it.measure(childConstraints) }
            val layoutWidth = constraints.maxWidth
            val layoutHeight = constraints.maxHeight
            layout(layoutWidth, layoutHeight) {
                placeables.forEach {
                    it.placeRelative(
                        x = (layoutWidth - it.width) / 2,
                        y = (layoutHeight - it.height) / 2
                    )
                }
            }
        }
    }
}

@Composable
private fun Image(modifier: Modifier = Modifier, qrCode: QRCode, contentDescription: String?) {
    Image(
        modifier = modifier,
        bitmap = qrCode.renderToBytes().decodeToImageBitmap(),
        contentDescription = contentDescription
    )
}