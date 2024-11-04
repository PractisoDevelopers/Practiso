package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
        LocalTextStyle provides MaterialTheme.typography.bodyMedium
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
    prefix: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {
        SingleLineTextShimmer(Modifier.fillMaxWidth())
    }
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        prefix()
        content()
    }
}

@Composable
fun OptionsFrameSkeleton(
    content: LazyListScope.() -> Unit = {
        items(3) {
            OptionSkeleton()
        }
    }
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingNormal)) {
        content(this)
    }
}

@Composable
private fun SingleLineTextShimmer(modifier: Modifier = Modifier) {
    Spacer(Modifier.height(LocalTextStyle.current.lineHeight.value.dp).then(modifier))
}
