package com.zhufucdev.practiso.composable

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

actual fun Modifier.actionTextClickable(
    primary: () -> Unit,
    secondary: () -> Unit
) = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = secondary,
        onClick = primary
    )
}