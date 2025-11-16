package com.zhufucdev.practiso.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NegativeText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = TextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
    softWarp: Boolean = true,
    shape: Shape = RectangleShape,
    maxLines: Int = Int.MAX_VALUE,
    margin: PaddingValues = PaddingValues.Zero,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val textMeasurer = rememberTextMeasurer()
    SubcomposeLayout(modifier) { constraints ->
        val measurement = textMeasurer.measure(
            text,
            style = style,
            overflow = overflow,
            softWrap = softWarp,
            maxLines = maxLines,
            constraints = constraints,
        )
        val canvas = subcompose("inverse-text-canvas") {
            val textSize = measurement.size.toSize().toDpSize()
            val leftMargin = margin.calculateLeftPadding(layoutDirection)
            val rightMargin = margin.calculateRightPadding(layoutDirection)
            val topMargin = margin.calculateTopPadding()
            val bottomMargin = margin.calculateBottomPadding()
            val paddedSize = DpSize(
                width = textSize.width + leftMargin + rightMargin,
                height = textSize.height + topMargin + bottomMargin
            )
            Canvas(
                Modifier.size(paddedSize)
                    .graphicsLayer {
                        compositingStrategy =
                            CompositingStrategy.Offscreen
                    }
                    .clip(shape)
            ) {
                drawRect(color)
                drawText(
                    measurement,
                    topLeft = Offset(leftMargin.toPx(), topMargin.toPx()),
                    color = Color.Black,
                    blendMode = BlendMode.Clear
                )
            }
        }
            .first()
            .measure(constraints)
        layout(canvas.width, canvas.height) {
            canvas.place(0, 0)
        }
    }
}


@Preview
@Composable
fun InverseTextPreview() {
    Surface(color = Color.Magenta) {
        NegativeText(
            text = "I am Batman",
            modifier = Modifier.padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            margin = PaddingValues(12.dp)
        )
    }
}