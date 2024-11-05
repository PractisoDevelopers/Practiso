package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun AlertHelper(
    header: @Composable () -> Unit,
    label: @Composable () -> Unit,
    helper: @Composable () -> Unit,
    modifier: Modifier? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier ?: Modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.displayLarge
        ) {
            header()
        }
        Spacer(Modifier.height(PaddingNormal))
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            label()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge
        ) {
            helper()
        }
    }
}