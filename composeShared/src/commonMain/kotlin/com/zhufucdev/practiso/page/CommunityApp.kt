package com.zhufucdev.practiso.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SingleLineTextShimmer
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.style.NotoEmojiFontFamily
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.CommunityAppViewModel
import kotlinx.coroutines.Dispatchers
import opacity.client.ArchiveMetadata
import opacity.client.DimensionMetadata
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.archives_para
import resources.dimensions_para
import resources.downloads_para
import resources.likes_para
import resources.n_questions_span
import resources.outline_download
import resources.outline_heart
import resources.show_all_span

@Composable
fun CommunityApp(model: CommunityAppViewModel = viewModel(factory = CommunityAppViewModel.Factory)) {
    val archives by model.archives.collectAsState(null, Dispatchers.IO)
    val dimensions by model.dimensions.collectAsState(null, Dispatchers.IO)

    LazyColumn {
        item {
            Row(
                Modifier.padding(horizontal = PaddingNormal).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionCaption(
                    pluralStringResource(
                        Res.plurals.dimensions_para,
                        dimensions?.size ?: 0
                    )
                )
                TextButton(onClick = {

                }) {
                    Text(stringResource(Res.string.show_all_span).uppercase())
                }
            }
        }

        item {
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
                contentPadding = PaddingValues(horizontal = PaddingNormal)
            ) {
                dimensions?.let {
                    it.forEach { dim ->
                        item(dim.name) {
                            DimensionCard(
                                model = dim,
                                onClick = {},
                                modifier = Modifier.width(DimensionCardWidth)
                            )
                        }
                    }
                } ?: repeat(5) {
                    item(it.toString()) {
                        DimensionCardSkeleton(Modifier.width(DimensionCardWidth))
                    }
                }
            }
            Spacer(Modifier.height(PaddingNormal))
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = PaddingNormal).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionCaption(pluralStringResource(Res.plurals.archives_para, archives?.size ?: 2))
            }
        }

        archives?.let { archives ->
            items(
                items = archives,
                key = { "archive#${it.id}" },
                contentType = { "archive" }) {
                OptionItem(modifier = Modifier.clickable(onClick = {})) {
                    ArchiveOption(
                        modifier = Modifier.fillMaxWidth(),
                        model = it
                    )
                }
            }
        } ?: items(5) {
            OptionItem {
                PractisoOptionSkeleton()
            }
        }
    }
}

private val DimensionCardWidth = 200.dp

@Composable
private fun DimensionCard(
    modifier: Modifier = Modifier,
    model: DimensionMetadata,
    onClick: () -> Unit,
) {
    DimensionCardSkeleton(
        modifier = modifier,
        onClick = onClick,
        emoji = {
            Text(model.emoji ?: "📝")
        },
        name = {
            Text(model.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        description = {
            Text(
                pluralStringResource(
                    Res.plurals.n_questions_span,
                    model.quizCount,
                    model.quizCount
                )
            )
        }
    )
}

@Composable
private fun DimensionCardSkeleton(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    emoji: @Composable () -> Unit = {
        val height = LocalTextStyle.current.lineHeight.value.dp
        Spacer(
            Modifier.size(width = height, height = height)
                .shimmerBackground()
        )
    },
    name: @Composable () -> Unit = {
        SingleLineTextShimmer(Modifier.fillMaxWidth())
    },
    description: @Composable () -> Unit = {
        SingleLineTextShimmer(Modifier.fillMaxWidth(fraction = 0.618f))
    },
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(PaddingNormal)) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = NotoEmojiFontFamily(),
                    fontWeight = FontWeight.Bold
                )
            ) {
                emoji()
            }
            Spacer(Modifier.height(PaddingNormal))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium
            ) {
                name()
            }
            Spacer(Modifier.height(PaddingSmall))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall
            ) {
                description()
            }
        }
    }
    if (onClick != null) {
        Card(modifier = modifier, onClick = onClick, content = content)
    } else {
        Card(modifier = modifier, content = content)
    }
}

@Composable
private fun ArchiveOption(modifier: Modifier = Modifier, model: ArchiveMetadata) {
    PractisoOptionSkeleton(
        modifier = modifier,
        label = {
            Text(model.name)
        },
        preview = {
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.outline_download),
                        contentDescription = stringResource(Res.string.downloads_para)
                    )
                    Text(model.downloads.toString())
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.outline_heart),
                        contentDescription = stringResource(Res.string.likes_para)
                    )
                    Text(model.likes.toString())
                }
            }
        }
    )
}

@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.padding(PaddingNormal)) { content() }
    }
    HorizontalSeparator()
}
