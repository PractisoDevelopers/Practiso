package com.zhufucdev.practiso.composition

import androidx.compose.ui.Modifier

expect fun Modifier.combineClickable(
    onClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
): Modifier
