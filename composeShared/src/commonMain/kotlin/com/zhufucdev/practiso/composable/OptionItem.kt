package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OptionItem(
    modifier: Modifier = Modifier,
    separator: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
    }
    if (separator) HorizontalSeparator()
}
