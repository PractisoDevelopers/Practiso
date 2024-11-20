package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composition.ScaleIndication
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun HorizontalPageIndicator(
    pageCount: Int,
    currentPage: Int,
    onSwitch: (Int) -> Unit,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            PaddingSmall,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        repeat(pageCount) { page ->
            val opacity by animateFloatAsState(if (currentPage == page) 1f else 0.5f)
            Box(
                Modifier.clickable(
                    interactionSource = null,
                    indication = ScaleIndication
                ) {
                    onSwitch(page)
                }
            ) {

                Spacer(
                    Modifier.size(size)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = opacity),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}