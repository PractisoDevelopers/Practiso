package com.zhufucdev.practiso.style

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import resources.NotoEmoji_Bold
import resources.Res

val AppTypography = Typography()

@Suppress("ComposableNaming")
@Composable
fun NotoEmojiFontFamily() = FontFamily(
    Font(resource = Res.font.NotoEmoji_Bold, weight = FontWeight.Bold)
)
