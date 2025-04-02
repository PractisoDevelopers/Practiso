package com.zhufucdev.practiso.composition

import androidx.compose.ui.Modifier

expect fun Modifier.combineClickable(
    onClick: (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
): Modifier
