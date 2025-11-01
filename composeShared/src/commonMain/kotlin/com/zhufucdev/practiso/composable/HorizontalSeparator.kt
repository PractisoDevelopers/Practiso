package com.zhufucdev.practiso.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalSeparator(modifier: Modifier = Modifier) {
    Spacer(
        Modifier.height(1.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) then modifier
    )
}
