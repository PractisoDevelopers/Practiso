package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun Modifier.stroker(
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 4.dp,
): Modifier {
    val transition = rememberInfiniteTransition("strokeTransition")
    val phase by transition.animateFloat(
        0f,
        20f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
    )
    return this then drawBehind {
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(10f, 10f),
                phase
            )
        )
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(PaddingNormal.toPx()),
            style = stroke
        )
    }
}