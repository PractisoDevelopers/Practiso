package com.zhufucdev.practiso.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.actionTextClickable(
    primary: () -> Unit,
    secondary: () -> Unit
) = composed {
    onClick(onDoubleClick = secondary, onClick = primary)
}