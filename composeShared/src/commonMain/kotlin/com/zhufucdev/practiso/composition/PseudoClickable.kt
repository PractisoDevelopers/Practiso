package com.zhufucdev.practiso.composition

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

fun Modifier.pseudoClickable(): Modifier = then(
    Modifier.clickable(
        interactionSource = null,
        indication = null,
        onClick = {})
)