package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun QuizSkeleton(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.fillMaxWidth().height(LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground(RoundedCornerShape(PaddingSmall))
        )
    },
    preview: @Composable () -> Unit = {
        repeat(2) {
            Spacer(
                Modifier.fillMaxWidth(0.6f).height(LocalTextStyle.current.lineHeight.value.dp)
                    .shimmerBackground(RoundedCornerShape(PaddingSmall))
            )
        }
    },
    tailingIcon: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall), modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMedium
            ) {
                label()
            }
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium
            ) {
                preview()
            }
        }

        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxHeight()) {
            tailingIcon()
        }
    }
}
