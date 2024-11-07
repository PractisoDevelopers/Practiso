package com.zhufucdev.practiso.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composition.combineClickable

@Composable
fun GlowingSurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
    glow: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        color = if (glow) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.combineClickable(
            onClick = onClick,
            onSecondaryClick = onSecondaryClick
        ),
        content = content
    )
}

