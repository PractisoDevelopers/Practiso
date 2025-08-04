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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.DownloadDispatcher
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SingleLineTextShimmer
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.datamodel.ArchiveHandle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.style.NotoEmojiFontFamily
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.CommunityAppViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import opacity.client.DimensionMetadata
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.archives_para
import resources.baseline_cloud_download
import resources.dimensions_para
import resources.download_and_import_para
import resources.downloads_para
import resources.likes_para
import resources.n_questions_span
import resources.outline_download
import resources.outline_heart
import resources.show_all_span

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CommunityApp(
    communityVM: CommunityAppViewModel = viewModel(factory = CommunityAppViewModel.Factory),
    importVM: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val archives by communityVM.archives.collectAsState(null, Dispatchers.IO)
    val dimensions by communityVM.dimensions.collectAsState(null, Dispatchers.IO)

    val scrollState = rememberLazyListState()
    val leadingItemIndex by remember(scrollState) { derivedStateOf { scrollState.firstVisibleItemIndex } }
    val isMountingNextPage by communityVM.isMountingNextPage.collectAsState()
    val hasNextPage by communityVM.hasNextPage.collectAsState(true)

    LaunchedEffect(leadingItemIndex, isMountingNextPage, hasNextPage) {
        if (!hasNextPage || isMountingNextPage) {
            return@LaunchedEffect
        }

        val lastIndexIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (archives?.let { it.size - lastIndexIndex > PRELOAD_EXTENT } != false) {
            return@LaunchedEffect
        }

        communityVM.mountNextPage()
    }

    LazyColumn(state = scrollState) {
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
                count = archives.size,
                key = { "archive#${archives[it].metadata.id}" },
                contentType = { "archive" }) { index ->
                OptionItem(
                    modifier = Modifier.clickable(onClick = {}),
                    separator = isMountingNextPage || index != archives.lastIndex
                ) {
                    ArchiveOption(
                        modifier = Modifier.fillMaxWidth(),
                        model = archives[index],
                        onDownloadRequest = {
                            importVM.viewModelScope.launch {
                                val handle = archives[index]
                                val pack = handle.download()
                                importVM.event.import.trySend(pack)
                                // remove cache afterwards
                                importVM.event.importComplete.first()
                                (DownloadDispatcher[handle.taskId].value?.takeIf { it is DownloadState.Completed } as DownloadState.Completed?)
                                    ?.destination
                                    ?.let {
                                        getPlatform().filesystem.delete(it)
                                        DownloadDispatcher[handle.taskId] = null
                                    }
                            }
                        }
                    )
                }
            }
        }

        if (archives == null || isMountingNextPage) {
            items(5) {
                OptionItem(separator = it != 4) {
                    PractisoOptionSkeleton()
                }
            }
        }
    }
}

private val DimensionCardWidth = 200.dp
private const val DEFAULT_DIMOJI = "📝"

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
            Text(model.emoji ?: DEFAULT_DIMOJI)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveOption(
    modifier: Modifier = Modifier,
    model: ArchiveHandle,
    onDownloadRequest: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        PractisoOptionSkeleton(
            label = {
                Text(
                    model.metadata.name.removeSuffix(".psarchive"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            preview = {
                Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                    ) {
                        model.metadata.dimensions.take(5).forEach {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    it.emoji ?: DEFAULT_DIMOJI,
                                    fontFamily = NotoEmojiFontFamily()
                                )
                                Text(
                                    "${it.name} (${it.quizCount})",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        val lineHeight = LocalTextStyle.current.lineHeight.value.dp
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(Res.drawable.outline_download),
                                contentDescription = stringResource(Res.string.downloads_para),
                                modifier = Modifier.size(lineHeight, lineHeight)
                            )
                            Text(model.metadata.downloads.toString())
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(Res.drawable.outline_heart),
                                contentDescription = stringResource(Res.string.likes_para),
                                modifier = Modifier.size(lineHeight, lineHeight)
                            )
                            Text(model.metadata.likes.toString())
                        }
                    }
                }
            }
        )
        PlainTooltipBox(
            text = stringResource(Res.string.download_and_import_para)
        ) {
            val downloadState by produceState<DownloadState?>(null) {
                withContext(Dispatchers.IO) {
                    DownloadDispatcher[model.taskId].collect {
                        value = it
                    }
                }
            }
            Box(Modifier.size(32.dp, 32.dp)) {
                when (val state = downloadState) {
                    null, is DownloadState.Completed -> IconButton(
                        onClick = onDownloadRequest,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            if (state == null) {
                                painterResource(Res.drawable.outline_download)
                            } else {
                                painterResource(Res.drawable.baseline_cloud_download)
                            },
                            contentDescription = stringResource(Res.string.download_and_import_para)
                        )
                    }

                    is DownloadState.Configure, is DownloadState.Preparing -> {
                        CircularProgressIndicator(Modifier.fillMaxSize())
                    }

                    is DownloadState.Downloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            progress = { state.progress }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    separator: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.padding(PaddingNormal)) { content() }
    }
    if (separator) HorizontalSeparator()
}

private const val PRELOAD_EXTENT = 3