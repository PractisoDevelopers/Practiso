package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.platform.createOptionView
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun PractisoOptionSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.height(LocalTextStyle.current.lineHeight.value.dp)
                .fillMaxWidth(fraction = 0.618f)
                .shimmerBackground()
        )
    },
    preview: (@Composable () -> Unit)? = {
        Spacer(
            Modifier.height(LocalTextStyle.current.lineHeight.value.dp)
                .fillMaxWidth()
                .shimmerBackground()
        )
    },
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingSmall),
        modifier = modifier
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleMedium
        ) {
            label()
        }

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            preview?.invoke()
        }

    }
}

@Composable
fun PractisoOptionView(
    option: PractisoOption,
    previewMaxLines: Int = 1,
    previewOverflow: TextOverflow = TextOverflow.Ellipsis,
    modifier: Modifier = Modifier,
) {
    val view = createOptionView(option)
    PractisoOptionSkeleton(
        label = { Text(view.title()) },
        preview = {
            Text(
                text = view.preview(),
                maxLines = previewMaxLines,
                overflow = previewOverflow
            )
        },
        modifier = modifier
    )
}