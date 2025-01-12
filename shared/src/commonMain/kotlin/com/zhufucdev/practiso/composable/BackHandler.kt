package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandlerOrIgnored(enabled: Boolean = true, onBack: () -> Unit)
