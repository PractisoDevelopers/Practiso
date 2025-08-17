package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
private fun ChipContent(label: @Composable () -> Unit, tailingIcon: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
        modifier = Modifier.padding(vertical = PaddingSmall, horizontal = PaddingNormal)
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            label()
        }
        tailingIcon()
    }
}

@Composable
fun ChipSkeleton(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.size(100.dp, LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    tailingIcon: @Composable () -> Unit = {},
) {
    val cardColors =
        if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    Card(
        colors = cardColors,
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.clip(CardDefaults.shape) then modifier
    ) { ChipContent(label, tailingIcon) }
}

