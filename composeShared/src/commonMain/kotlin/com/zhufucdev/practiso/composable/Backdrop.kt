package com.zhufucdev.practiso.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

const val BackdropKey = "backdrop"

@Composable
fun Backdrop(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Spacer(
        Modifier.fillMaxSize().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        ) then modifier
    )
}