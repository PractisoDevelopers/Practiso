package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable

@Composable
fun FooterColumn(content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeables = measurables.reversed().drop(1).runningFold(
            constraints to measurables.last().measure(constraints)
        ) { (previousConstraints, m), measurable ->
            val c =
                previousConstraints.copy(maxHeight = previousConstraints.maxHeight - m.height)
            c to measurable.measure(c)
        }.map(Pair<*, Placeable>::second)
        val layoutWidth = placeables.maxOf(Placeable::width)
        val layoutHeight = placeables.sumOf(Placeable::height)
        var remainingHeight = layoutHeight
        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                it.placeRelative(0, remainingHeight - it.height)
                remainingHeight -= it.height
            }
        }
    }
}