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
import androidx.compose.runtime.LaunchedEffect
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
import com.zhufucdev.practiso.composition.ExclusionLock
import com.zhufucdev.practiso.composition.withExclusionLock
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val SharedHorizontalDraggableExclusion = ExclusionLock()

@Composable
fun HorizontalDraggable(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    lock: ExclusionLock = SharedHorizontalDraggableExclusion,
    controls: HorizontalDraggableScope.() -> Unit,
    content: @Composable () -> Unit,
) = withExclusionLock(lock) {
    val dragAnimator = remember { Animatable(0f) }
    val coroutine = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var targetWidth by remember(controls) { mutableFloatStateOf(0f) }
    var paddedWidth by remember(controls) { mutableFloatStateOf(0f) }

    LaunchOstracization {
        dragAnimator.animateTo(0f) {
            dragOffset = value
        }
    }

    fun onDragEnd() {
        coroutine.launch {
            dragAnimator.snapTo(dragOffset)
            if (-dragOffset.dp > ((targetWidth + paddedWidth) * 0.5f).dp) {
                dragAnimator.animateTo(-(targetWidth + paddedWidth)) {
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
                    if (!enabled || targetWidth <= 0) {
                        return@pointerInput
                    }

                    detectHorizontalDragGestures(
                        onDragStart = {
                            coroutine.launch {
                                lock()
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            if (dragOffset >= 0 && amount > 0) {
                                return@detectHorizontalDragGestures
                            }
                            dragOffset += amount.toDp().value
                            change.consume()
                        },
                        onDragCancel = {
                            onDragEnd()
                        },
                        onDragEnd = {
                            onDragEnd()
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
                val items = remember(controls) {
                    buildList {
                        controls(object : HorizontalDraggableScope, RowScope by this@Row {
                            override fun item(
                                width: Dp,
                                content: @Composable (() -> Unit)
                            ) {
                                targetWidth += width.value
                                add(content)
                            }
                        })
                    }
                }
                LaunchedEffect(items) {
                    paddedWidth = PaddingSmall.value * (items.size + 1)
                }
                items.forEach { it() }
            }
        }
    }
}

val HorizontalDraggingControlTargetWidth = 80.dp

interface HorizontalDraggableScope : RowScope {
    fun item(width: Dp, content: @Composable () -> Unit)
}

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

