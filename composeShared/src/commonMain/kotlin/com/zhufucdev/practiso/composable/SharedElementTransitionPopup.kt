package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.SharedElementTransitionPopupViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

const val SharedElementTransitionKey = "shared_element_transition"

interface SharedElementTransitionPopupScope {
    fun Modifier.sharedElement(): Modifier
    suspend fun expand()
    suspend fun collapse()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementTransitionPopup(
    key: String,
    model: SharedElementTransitionPopupViewModel = viewModel(
        key = key,
        factory = SharedElementTransitionPopupViewModel.Factory
    ),
    popup: @Composable SharedElementTransitionPopupScope.() -> Unit,
    sharedElement: @Composable (Modifier) -> Unit,
    content: @Composable SharedElementTransitionPopupScope.() -> Unit,
) {
    val coroutine = rememberCoroutineScope()
    val scopeImpl = object : SharedElementTransitionPopupScope {
        override fun Modifier.sharedElement(): Modifier =
            this then Modifier.onGloballyPositioned {
                model.transitionStart = it.boundsInRoot()
            }
                .alpha(if (model.visible) 0f else 1f)

        override suspend fun expand() {
            model.expand()
        }

        override suspend fun collapse() {
            model.collapse()
        }
    }

    if (model.visible) {
        composeFromBottomUp(SharedElementTransitionKey) {
            SharedTransitionScope { mod ->
                val maskAlpha by animateFloatAsState(
                    if (model.expanded) 0.5f else 0f
                )
                Box(
                    mod.fillMaxSize().background(Color.Black.copy(alpha = maskAlpha))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { coroutine.launch { model.collapse() } }
                        )
                ) {
                    AnimatedVisibility(
                        model.expanded,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        val releaseAnimator = remember { Animatable(0f) }
                        val localCoroutine = rememberCoroutineScope()

                        suspend fun release(velocityTracker: VelocityTracker) {
                            val velocity = velocityTracker.calculateVelocity().y
                            if (abs(releaseAnimator.value) > 40 || velocity > 10) {
                                model.collapse()
                            } else {
                                releaseAnimator.animateTo(0f)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .padding(PaddingNormal)
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key),
                                    animatedVisibilityScope = this@AnimatedVisibility
                                )
                                .offset(y = releaseAnimator.value.dp)
                                .pointerInput(true) {
                                    val velocityTracker = VelocityTracker()
                                    detectVerticalDragGestures(
                                        onDragEnd = {
                                            localCoroutine.launch {
                                                release(velocityTracker)
                                            }
                                        },
                                        onDragCancel = {
                                            localCoroutine.launch {
                                                release(velocityTracker)
                                            }
                                        },
                                        onVerticalDrag = { change, amount ->
                                            if (!change.isConsumed) {
                                                change.consume()
                                                localCoroutine.launch {
                                                    releaseAnimator.snapTo(releaseAnimator.value + amount.toDp().value)
                                                }
                                                velocityTracker.addPosition(
                                                    change.uptimeMillis,
                                                    change.position
                                                )
                                            }
                                        }
                                    )
                                }
                        ) {
                            popup(scopeImpl)
                        }
                    }

                    AnimatedVisibility(
                        !model.expanded,
                        modifier = Modifier.offset {
                            IntOffset(
                                model.transitionStart.left.toInt(),
                                model.transitionStart.top.toInt()
                            )
                        }
                            .size(with(LocalDensity.current) { model.transitionStart.size.toDpSize() })
                    ) {
                        sharedElement(
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key),
                                animatedVisibilityScope = this@AnimatedVisibility
                            ),
                        )
                    }
                }
            }
        }
    }

    content(scopeImpl)
}