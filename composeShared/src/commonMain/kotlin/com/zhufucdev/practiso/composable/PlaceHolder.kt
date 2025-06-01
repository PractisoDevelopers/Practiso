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
import androidx.compose.ui.text.style.TextAlign
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun PlaceHolder(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    label: @Composable () -> Unit,
    helper: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize() then modifier
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.displayLarge
        ) {
            header()
        }
        Spacer(Modifier.height(PaddingNormal))
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
        ) {
            label()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
        ) {
            helper()
        }
    }
}