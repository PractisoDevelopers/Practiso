package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun DimensionSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.size(100.dp, LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    tailingIcon: @Composable () -> Unit = {},
) {
    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
            modifier = Modifier.padding(PaddingSmall)
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge
            ) {
                label()
            }
            tailingIcon()
        }
    }
}

