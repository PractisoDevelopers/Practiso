package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.composable.AppExceptionAlert
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SingleLineTextShimmer
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.datamodel.ArchiveHandle
import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadStopped
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.ImportState
import com.zhufucdev.practiso.style.NotoEmojiFontFamily
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.CommunityAppViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import opacity.client.ArchiveMetadata
import opacity.client.DimensionMetadata
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.archives_para
import resources.baseline_cloud_download
import resources.cancel_para
import resources.details_para
import resources.dimensions_para
import resources.download_and_import_para
import resources.download_error_para
import resources.downloads_para
import resources.failed_to_download_archive_para
import resources.likes_para
import resources.n_questions_span
import resources.outline_download
import resources.outline_heart
import resources.refresh_para
import resources.retry_para
import resources.show_all_span
import resources.something_went_wrong_para
import resources.use_another_server_or_try_again_later_para

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CommunityApp(
    communityVM: CommunityAppViewModel = viewModel(factory = CommunityAppViewModel.Factory),
    importVM: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val pageState by communityVM.pageState.collectAsState()
    AnimatedContent(pageState) { ps ->
        when (ps) {
            is CommunityAppViewModel.Failed -> FailurePage(communityVM.event.refresh)
            else -> DefaultPage(communityVM, importVM)
        }
    }
}

@Composable
private fun DefaultPage(communityVM: CommunityAppViewModel, importVM: ImportViewModel) {
    val archives by communityVM.archives.collectAsState(null, Dispatchers.IO)
    val dimensions by communityVM.dimensions.collectAsState(null, Dispatchers.IO)
    val snackbars = LocalExtensiveSnackbarState.current

    val scrollState = rememberLazyListState()
    val leadingItemIndex by remember(scrollState) { derivedStateOf { scrollState.firstVisibleItemIndex } }
    val isMountingNextPage by communityVM.isMountingNextPage.collectAsState()
    val hasNextPage by communityVM.hasNextPage.collectAsState(true)
    val pageScope = rememberCoroutineScope()

    var errorModel by remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(leadingItemIndex, isMountingNextPage, hasNextPage) {
        if (!hasNextPage || isMountingNextPage) {
            return@LaunchedEffect
        }

        val lastIndexIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (archives?.let { it.size - lastIndexIndex > PRELOAD_EXTENT } != false) {
            return@LaunchedEffect
        }

        communityVM.event.mountNextPage.send(Unit)
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
                val handle = archives[index]
                val state by communityVM.downloadManager.flatMapMerge {
                    it[handle.taskId]
                }.collectAsState(DownloadStopped.Idle)

                OptionItem(
                    modifier = Modifier.clickable(onClick = {}),
                    separator = isMountingNextPage || index != archives.lastIndex
                ) {
                    ArchiveOption(
                        modifier = Modifier.fillMaxWidth(),
                        model = handle.metadata,
                        state = state,
                        onDownloadRequest = {
                            pageScope.launch {
                                try {
                                    downloadAndImport(
                                        handle = archives[index],
                                        downloadManager = communityVM.downloadManager.first(),
                                        importVmEvents = importVM.event
                                    )
                                } catch (_: CancellationException) {

                                } catch (e: Exception) {
                                    val action = snackbars.showSnackbar(
                                        message = getString(Res.string.failed_to_download_archive_para),
                                        actionLabel = getString(Res.string.details_para)
                                    )
                                    if (action == SnackbarResult.ActionPerformed) {
                                        errorModel = e
                                    }
                                }
                            }
                        },
                        onErrorDetailsRequest = {
                            errorModel = it
                        },
                        onCancelRequest = {
                            val taskId = archives[index].taskId
                            pageScope.launch {
                                communityVM.downloadManager.first().cancel(taskId)
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

    errorModel?.let { model ->
        AppExceptionAlert(
            model = model,
            onDismissRequest = { errorModel = null },
        )
    }
}

@Composable
private fun FailurePage(refresh: SendChannel<Unit>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PaddingSmall, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaceHolder(
            header = {
                Text("🛜")
            },
            label = {
                Text(
                    stringResource(Res.string.something_went_wrong_para),
                )
            },
            helper = {
                Text(
                    stringResource(Res.string.use_another_server_or_try_again_later_para),
                )
            }
        )

        Button(
            onClick = {
                refresh.trySend(Unit)
            }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(Res.string.refresh_para))
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
    model: ArchiveMetadata,
    state: DownloadCycle,
    onDownloadRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onErrorDetailsRequest: (Exception) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        PractisoOptionSkeleton(
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    model.name.removeSuffix(".psarchive"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            preview = {
                AnimatedContent(state, modifier = Modifier.fillMaxWidth(), transitionSpec = {
                    if (state is DownloadState.Downloading) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        defaultAnimatedContentSpecs
                    }
                }) { state ->
                    when (state) {
                        is DownloadStopped.Idle,
                        is DownloadState.Completed,
                            -> {
                            Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                                FlowRow(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                                ) {
                                    model.dimensions.take(5).forEach {
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
                                        Text(model.downloads.toString())
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painterResource(Res.drawable.outline_heart),
                                            contentDescription = stringResource(Res.string.likes_para),
                                            modifier = Modifier.size(lineHeight, lineHeight)
                                        )
                                        Text(model.likes.toString())
                                    }
                                }
                            }
                        }

                        is DownloadStopped.Error -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                                Text(stringResource(Res.string.download_error_para))
                                Text(
                                    stringResource(Res.string.details_para),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = {
                                        onErrorDetailsRequest(state.model)
                                    })
                                )
                            }
                        }

                        is DownloadState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        is DownloadState.Preparing,
                        is DownloadState.Configure,
                            -> {
                            LinearProgressIndicator(Modifier.weight(1f))
                        }
                    }
                }
            }
        )
        AnimatedContent(state, transitionSpec = {
            if (state is DownloadState.Downloading) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                defaultAnimatedContentSpecs
            }
        }) { state ->
            Box(Modifier.size(32.dp, 32.dp)) {
                when (state) {
                    is DownloadState.Completed -> IconButton(
                        onClick = onDownloadRequest,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painterResource(Res.drawable.outline_download),
                            contentDescription = stringResource(Res.string.download_and_import_para)
                        )
                    }

                    is DownloadStopped.Idle ->
                        PlainTooltipBox(
                            text = stringResource(Res.string.download_and_import_para)
                        ) {
                            IconButton(
                                onClick = onDownloadRequest,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    painterResource(Res.drawable.baseline_cloud_download),
                                    contentDescription = null
                                )
                            }
                        }

                    is DownloadStopped.Error ->
                        PlainTooltipBox(text = stringResource(Res.string.retry_para)) {
                            IconButton(
                                onClick = onDownloadRequest,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null
                                )
                            }
                        }

                    is DownloadState -> PlainTooltipBox(stringResource(Res.string.cancel_para)) {
                        IconButton(
                            onClick = onCancelRequest,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
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

private suspend fun downloadAndImport(
    handle: ArchiveHandle,
    downloadManager: DownloadManager,
    importVmEvents: ImportViewModel.Events,
) {
    val pack = handle.getAsSource(downloadManager)
    importVmEvents.import.trySend(pack)

    // remove cache afterwards
    if (importVmEvents.importFinish.first() == ImportState.IdleReason.Completion) {
        (downloadManager[handle.taskId].value.takeIf { it is DownloadState.Completed } as DownloadState.Completed?)
            ?.destination
            ?.let {
                getPlatform().filesystem.delete(it)
            }
        downloadManager.cancel(handle.taskId)
    }
}

private val defaultAnimatedContentSpecs =
    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
        .togetherWith(fadeOut(animationSpec = tween(90)))
