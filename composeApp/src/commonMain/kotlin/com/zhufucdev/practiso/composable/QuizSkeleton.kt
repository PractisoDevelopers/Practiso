package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun QuizSkeleton(
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
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall), modifier = modifier) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            label()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            preview()
        }
    }
}
