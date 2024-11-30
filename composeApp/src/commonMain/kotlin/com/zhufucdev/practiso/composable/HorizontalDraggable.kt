package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HorizontalDraggable(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    targetWidth: Dp,
    controls: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val dragAnimator = remember { Animatable(0f) }
    val coroutine = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun onDropEnd() {
        coroutine.launch {
            dragAnimator.snapTo(dragOffset)
            if (-dragOffset.dp > targetWidth * 0.5f) {
                dragAnimator.animateTo(-targetWidth.value) {
                    dragOffset = value
                }
            } else {
                dragAnimator.animateTo(0f) {
                    dragOffset = value
                }
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Surface(
            Modifier.fillMaxWidth().offset(x = dragOffset.dp)
                .pointerInput(enabled) {
                    if (!enabled) {
                        return@pointerInput
                    }

                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            if (dragOffset >= 0 && amount > 0) {
                                return@detectHorizontalDragGestures
                            }
                            dragOffset += amount.toDp().value
                            change.consume()
                        },
                        onDragCancel = {
                            onDropEnd()
                        },
                        onDragEnd = {
                            onDropEnd()
                        }
                    )
                }
        ) {
            content()
        }

        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Row(
                Modifier.fillMaxHeight().width(-dragOffset.dp)
                    .padding(PaddingSmall),
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                controls()
            }
        }
    }
}

val HorizontalDraggingControlTargetWidth = 80.dp

@Composable
fun RowScope.HorizontalControl(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    targetWidth: Dp = HorizontalDraggingControlTargetWidth,
    color: Color,
    content: @Composable () -> Unit,
) {
    val targetHalfPx = with(LocalDensity.current) { (targetWidth / 2).roundToPx() }
    Layout(
        modifier = Modifier.weight(1f).fillMaxHeight().background(
            shape = shape,
            color = color
        ).clip(shape).clipToBounds() then modifier,
        content = content
    ) { measurables, constraints ->
        val childConstraints = Constraints(
            maxHeight = (constraints.maxHeight * 0.4).roundToInt()
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                val y = ((layoutHeight - it.height) / 2f).roundToInt()
                if (layoutWidth < targetHalfPx * 2) {
                    it.placeRelative(layoutWidth - targetHalfPx - (it.width / 2f).roundToInt(), y)
                } else {
                    it.placeRelative(((layoutWidth - it.width) / 2f).roundToInt(), y)
                }
            }
        }
    }
}

