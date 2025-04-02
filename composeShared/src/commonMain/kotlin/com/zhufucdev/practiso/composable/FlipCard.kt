package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    state: FlipCardState = remember { FlipCardState() },
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    Card(
        modifier = modifier then Modifier.graphicsLayer {
            rotationY = state.currentRotationDegrees
            cameraDistance = size.width / 10
        },
        shape = shape,
        colors = colors
    ) {
        content(state.currentPage)
    }
}

@Stable
class FlipCardState(initialPage: Int = 0) {
    var currentPage: Int by mutableIntStateOf(initialPage)
        private set
    private val animator = Animatable(0f)
    val currentRotationDegrees by animator.asState()

    suspend fun flip(targetPage: Int) {
        val ahead = targetPage > currentPage
        animator.animateTo(if (ahead) 90f else -90f, tween(easing = LinearEasing))
        currentPage = targetPage
        animator.snapTo(if (ahead) 270f else -270f)
        animator.animateTo(if (ahead) 360f else -360f, tween(easing = LinearEasing))
        animator.snapTo(0f)
    }

    suspend fun snap(targetPage: Int) {
        currentPage = targetPage
        animator.snapTo(0f)
    }

    suspend fun flipNext() {
        flip(currentPage + 1)
    }
}