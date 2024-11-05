package com.zhufucdev.practiso.composition

import androidx.compose.ui.Modifier

actual fun Modifier.secondaryClickable(onClick: () -> Unit) = longClickable(onClick)