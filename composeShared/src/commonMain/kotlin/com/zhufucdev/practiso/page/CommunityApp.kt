@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.DEFAULT_DIMOJI
import com.zhufucdev.practiso.composable.AppExceptionAlert
import com.zhufucdev.practiso.composable.ArchiveMetadataOption
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SingleLineTextShimmer
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.style.NotoEmojiFontFamily
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.uiSharedId
import com.zhufucdev.practiso.viewmodel.CommunityAppViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import opacity.client.DimensionMetadata
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.archives_para
import resources.details_para
import resources.dimensions_para
import resources.failed_to_download_archive_para
import resources.n_questions_span
import resources.refresh_para
import resources.show_all_span
import resources.something_went_wrong_para
import resources.use_another_server_or_try_again_later_para

@Composable
fun CommunityApp(
    communityVM: CommunityAppViewModel = viewModel(factory = CommunityAppViewModel.Factory),
    importVM: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
    sharedTransition: SharedTransitionScope,
    animatedContent: AnimatedContentScope,
) {
    val pageState by communityVM.pageState.collectAsState()
    AnimatedContent(pageState) { ps ->
        when (ps) {
            is CommunityAppViewModel.Failed -> FailurePage(communityVM.event.refresh)
            else -> DefaultPage(communityVM, importVM, sharedTransition, animatedContent)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun DefaultPage(
    communityVM: CommunityAppViewModel,
    importVM: ImportViewModel,
    sharedTransition: SharedTransitionScope,
    animatedContent: AnimatedContentScope,
) {
    val archives by communityVM.archives.collectAsState(null, Dispatchers.IO)
    val dimensions by communityVM.dimensions.collectAsState(null, Dispatchers.IO)
    val snackbars = LocalExtensiveSnackbarState.current
    val navController = LocalNavController.current!!

    val scrollState = rememberLazyListState()

    val leadingItemIndex by remember(scrollState) { derivedStateOf { scrollState.firstVisibleItemIndex } }
    val isMountingNextPage by communityVM.isMountingNextPage.collectAsState()
    val hasNextPage by communityVM.hasNextPage.collectAsState(true)
    val downloadError by communityVM.downloadError.collectAsState()

    var alertError by remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(leadingItemIndex, isMountingNextPage, hasNextPage) {
        // pagination
        if (!hasNextPage || isMountingNextPage) {
            return@LaunchedEffect
        }

        val lastIndexIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (archives?.let { it.size - lastIndexIndex > PRELOAD_EXTENT } != false) {
            return@LaunchedEffect
        }

        communityVM.event.mountNextPage.send(Unit)
    }

    LaunchedEffect(downloadError) {
        if (downloadError == null) {
            return@LaunchedEffect
        }

        val action = snackbars.showSnackbar(
            message = getString(Res.string.failed_to_download_archive_para),
            actionLabel = getString(Res.string.details_para)
        )
        if (action == SnackbarResult.ActionPerformed) {
            alertError = downloadError
        }
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
                key = { "archive#${archives[it].id}" },
                contentType = { "archive" }) { index ->
                val archive = archives[index]
                val state by communityVM.getDownloadStateFlow(archive).collectAsState()

                OptionItem(
                    modifier = Modifier.clickable(onClick = {
                        navController.navigate(ArchivePreviewRouteParams(archive))
                    }),
                    separator = isMountingNextPage || index != archives.lastIndex
                ) {
                    ArchiveMetadataOption(
                        modifier = with(sharedTransition) {
                            Modifier.fillMaxWidth()
                                .sharedElement(
                                    sharedTransition.rememberSharedContentState(archive.uiSharedId),
                                    animatedVisibilityScope = animatedContent
                                )
                        },
                        model = archive,
                        state = state,
                        onDownloadRequest = {
                            communityVM.archiveEvent.downloadAndImport.trySend(
                                archive to importVM.event
                            )
                        },
                        onErrorDetailsRequest = {
                            alertError = it
                        },
                        onCancelRequest = {
                            communityVM.archiveEvent.cancelDownload.trySend(archive)
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

    alertError?.let { model ->
        AppExceptionAlert(
            model = model,
            onDismissRequest = {
                communityVM.archiveEvent.clearDownloadError.trySend(Unit)
                alertError = null
            },
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
