package com.zhufucdev.practiso.composition

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OpacityIndication(val opacityPressed: Float = 0.5f) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return OpacityNode(interactionSource, opacityPressed)
    }

    override fun equals(other: Any?) =
        other is OpacityIndication && other.opacityPressed == opacityPressed

    override fun hashCode(): Int = 32 * opacityPressed.hashCode()
}

private class OpacityNode(
    private val interactionSource: InteractionSource,
    private val opacityPressed: Float,
) : Modifier.Node(), LayoutModifierNode {
    val opacity = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest {
                when (it) {
                    is PressInteraction.Press -> opacity.animateTo(opacityPressed)
                    is PressInteraction.Cancel, is PressInteraction.Release -> opacity.animateTo(1f)
                }
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                alpha = opacity.value
            }
        }
    }
}
