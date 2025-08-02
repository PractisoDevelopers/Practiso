package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SingleLineTextShimmer(modifier: Modifier = Modifier) {
    Spacer(
        Modifier.shimmerBackground().height(LocalTextStyle.current.lineHeight.value.dp)
            .then(modifier)
    )
}
