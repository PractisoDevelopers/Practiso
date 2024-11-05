package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun TextFrameSkeleton(
    content: @Composable () -> Unit = {
        Column {
            SingleLineTextShimmer(Modifier.fillMaxWidth())
            SingleLineTextShimmer(Modifier.fillMaxWidth(fraction = 0.618f))
            SingleLineTextShimmer(Modifier.fillMaxWidth(fraction = 0.618f))
        }
    },
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyLarge
    ) {
        content()
    }
}

@Composable
fun ImageFrameSkeleton(
    image: @Composable () -> Unit = {
        Spacer(Modifier.fillMaxWidth().height(150.dp))
    },
    altText: @Composable () -> Unit = {
        SingleLineTextShimmer(Modifier.fillMaxWidth(fraction = 0.618f))
    },
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingNormal),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        image()
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelMedium
        ) {
            altText()
        }
    }
}

@Composable
fun OptionSkeleton(
    modifier: Modifier = Modifier,
    prefix: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {
        SingleLineTextShimmer(Modifier.fillMaxWidth())
    },
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        prefix()
        content()
    }
}

@Composable
fun OptionsFrameSkeleton(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {
        repeat(3) {
            OptionSkeleton()
        }
    },
) {
    Column {
        Box(Modifier.fillMaxWidth()) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge
            ) {
                label()
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(PaddingSmall),
            modifier = modifier
        ) {
            content(this)
        }
    }
}

@Composable
private fun SingleLineTextShimmer(modifier: Modifier = Modifier) {
    Spacer(Modifier.height(LocalTextStyle.current.lineHeight.value.dp).then(modifier))
}
