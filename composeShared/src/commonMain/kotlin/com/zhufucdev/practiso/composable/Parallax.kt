package com.zhufucdev.practiso.composable

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
fun Parallax(
    modifier: Modifier = Modifier,
    nestedScrollConnection: ParallaxScrollConnection,
    content: @Composable () -> Unit,
) {
    val connection = nestedScrollConnection as ParallaxScrollConnectionImpl
    SubcomposeLayout(
        modifier.clipToBounds()
    ) { constraints ->
        val bounds = subcompose("parallax-content", content).first()
            .measure(constraints.copy(maxHeight = Constraints.Infinity))
        connection.bounds = IntSize(bounds.width, bounds.height)

        val (shrinkX, shrinkY) = connection.shrink.x.roundToInt() to connection.shrink.y.roundToInt()
        val (offsetX, offsetY) = connection.offset.x.roundToInt() to connection.offset.y.roundToInt()
        layout(bounds.width - shrinkX, bounds.height - shrinkY) {
            bounds.place(offsetX, offsetY)
        }
    }
}

sealed interface ParallaxScrollConnection : NestedScrollConnection

private class ParallaxScrollConnectionImpl(
    val shrinkRatioX: Float,
    val shrinkRatioY: Float,
    private val shouldScroll: ((Float) -> Boolean)? = null,
) : ParallaxScrollConnection {
    var bounds = IntSize.Zero
    var shrink by mutableStateOf(Offset.Zero)
    var offset by mutableStateOf(Offset.Zero)
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if ((shrinkRatioY > 0 || shrinkRatioX > 0)
            && shouldScroll?.invoke(
                (shrinkRatioY + shrinkRatioX).let { total ->
                    shrink.x / bounds.width * shrinkRatioX / total + shrink.y / bounds.height * shrinkRatioY / total
                }
            ) == false
        ) {
            return Offset.Zero
        }

        var nextShrink = shrink - available.let { Offset(it.x * shrinkRatioX, it.y * shrinkRatioY) }
        if (nextShrink.x > bounds.width) {
            nextShrink = nextShrink.copy(x = bounds.width.toFloat())
        } else if (nextShrink.x < 0) {
            nextShrink = nextShrink.copy(x = 0f)
        }
        if (nextShrink.y > bounds.height) {
            nextShrink = nextShrink.copy(y = bounds.height.toFloat())
        } else if (nextShrink.y < 0) {
            nextShrink = nextShrink.copy(y = 0f)
        }
        try {
            return shrink - nextShrink
        } finally {
            shrink = nextShrink
            offset = -nextShrink / 2f
        }
    }
}

private class ParallaxScrollState : ScrollableState {
    override val isScrollInProgress: Boolean
        get() = false

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return delta
    }

}

fun ParallaxScrollConnection(
    strengthHorizontal: Float = 0f,
    strengthVertical: Float = 1f,
    shouldScroll: ((collapseProgress: Float) -> Boolean)? = null,
): ParallaxScrollConnection =
    ParallaxScrollConnectionImpl(strengthHorizontal, strengthVertical, shouldScroll)
