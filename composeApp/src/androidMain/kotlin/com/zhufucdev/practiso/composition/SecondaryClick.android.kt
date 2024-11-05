package com.zhufucdev.practiso.composition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.combineClickable(
    onClick: () -> Unit,
    onSecondaryClick: (() -> Unit)?,
): Modifier = combinedClickable(onClick = onClick, onLongClick = onSecondaryClick)