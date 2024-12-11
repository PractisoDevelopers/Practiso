package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun DialogContentSkeleton(
    modifier: Modifier = Modifier,
    icon: @Composable ColumnScope.() -> Unit,
    title: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingNormal)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            icon()
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
            title()
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            content()
        }
    }
}