package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun OptionItem(
    modifier: Modifier = Modifier,
    separator: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.padding(PaddingNormal)) { content() }
    }
    if (separator) HorizontalSeparator()
}
