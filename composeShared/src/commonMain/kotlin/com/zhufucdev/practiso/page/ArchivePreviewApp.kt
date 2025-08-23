package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.DEFAULT_DIMOJI
import com.zhufucdev.practiso.composable.AppExceptionAlert
import com.zhufucdev.practiso.composable.ArchiveMetadataOption
import com.zhufucdev.practiso.composable.ChipSkeleton
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.NoGroup
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SomeGroup
import com.zhufucdev.practiso.composable.filter
import com.zhufucdev.practiso.composable.filteredItems
import com.zhufucdev.practiso.composable.rememberFilterController
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.uiSharedId
import com.zhufucdev.practiso.viewmodel.ArchivePreviewViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import opacity.client.ArchivePreview
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import resources.Res
import resources.stranded_quizzes_para
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun ArchivePreviewApp(
    previewVM: ArchivePreviewViewModel = viewModel(factory = ArchivePreviewViewModel.Factory),
    importVM: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
    sharedTransition: SharedTransitionScope,
    animatedContent: AnimatedContentScope,
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val archive by previewVM.archive.collectAsState()
    val preview by previewVM.preview.collectAsState(null)
    val downloadError by previewVM.downloadError.collectAsState()
    val filterController = rememberFilterController(
        items = preview ?: emptyList(),
        groupSelector = ArchivePreview::dimensions
    )
    LaunchedEffect(filterController.groupedItems) {
        filterController.groupedItems.keys.forEach {
            filterController.toggleGroup(it, selected = true)
        }
    }

    Scaffold(
        topBar = {
            archive?.let { archive ->
                TextCanvas(
                    modifier = Modifier.fillMaxWidth().height(240.dp).alpha(0.73f),
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 48.sp,
                    ),
                    textAndCount = archive.dimensions.map {
                        (it.emoji ?: DEFAULT_DIMOJI) to it.quizCount
                    }.toTypedArray(),
                    verticalSpacing = PaddingBig
                )
            } ?: Box(
                Modifier.height(240.dp).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
        },
    ) { safeArea ->
        LazyColumn(Modifier.padding(safeArea)) {
            item {
                Card(Modifier.padding(PaddingNormal)) {
                    archive?.let { archive ->
                        val state by previewVM.getDownloadStateFlow(archive)
                            .collectAsState()
                        ArchiveMetadataOption(
                            modifier = with(sharedTransition) {
                                Modifier.fillParentMaxWidth()
                                    .padding(vertical = PaddingNormal, horizontal = PaddingBig * 2)
                                    .sharedElement(
                                        sharedTransition.rememberSharedContentState(archive.uiSharedId),
                                        animatedVisibilityScope = animatedContent
                                    )
                            },
                            model = archive,
                            state = state,
                            onDownloadRequest = {
                                previewVM.archiveEvent.downloadAndImport.trySend(
                                    archive to importVM.event
                                )
                            },
                            onCancelRequest = {
                                previewVM.archiveEvent.cancelDownload.trySend(archive)
                            },
                            onErrorDetailsRequest = {
                                // noop
                            }
                        )
                    } ?: PractisoOptionSkeleton(Modifier.fillParentMaxWidth())
                }
            }

            if (filterController.groupedItems.size > 1) {
                // concrete data has been fetched
                filter(
                    Modifier.background(backgroundColor),
                    contentPadding = PaddingValues(horizontal = PaddingNormal),
                    controller = filterController,
                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall)
                ) { group ->
                    ChipSkeleton(
                        modifier = Modifier.clickable(onClick = { filterController.toggleGroup(group) }),
                        selected = group in filterController.selectedGroups,
                        label = {
                            Text(
                                when (group) {
                                    is SomeGroup<String> -> group.value
                                    is NoGroup -> stringResource(Res.string.stranded_quizzes_para)
                                }
                            )
                        }
                    )
                }
            } else if (archive?.dimensions?.let { it.size > 1 } != false) {
                // preloaded but still fetching,
                // and the user can expect to see stuff here
                item {
                    LazyRow(
                        contentPadding = PaddingValues(PaddingNormal),
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        items(5) {
                            ChipSkeleton()
                        }
                    }
                }
            }

            if (preview != null) {
                filteredItems(
                    controller = filterController,
                ) { items, index ->
                    PractisoOptionSkeleton(
                        Modifier.padding(PaddingNormal),
                        label = {
                            Text(items[index].name)
                        },
                        preview = {
                            Text(
                                items[index].body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    if (index < items.size - 1) {
                        HorizontalSeparator()
                    }
                }
            } else {
                items(count = 5) {
                    PractisoOptionSkeleton(Modifier.padding(PaddingNormal))
                    if (it < 4) {
                        HorizontalSeparator()
                    }
                }
            }
        }
    }

    downloadError?.let { model ->
        AppExceptionAlert(
            model = model,
            onDismissRequest = { previewVM.archiveEvent.clearDownloadError.trySend(Unit) },
        )
    }
}

@Composable
private fun TextCanvas(
    modifier: Modifier = Modifier,
    vararg textAndCount: Pair<String, Int>,
    learnDegrees: Float = 15f,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    horizontalSpacing: Dp = PaddingNormal,
    verticalSpacing: Dp = PaddingNormal,
    random: Random = Random(seed = textAndCount.size),
) {
    val textMeasurer = rememberTextMeasurer()
    val textAndAccumulatedCount = remember(textAndCount) {
        buildList {
            var accumulator = 0
            for (pair in textAndCount) {
                accumulator += pair.second
                add(pair.first to accumulator)
            }
        }
    }
    val layoutByText = remember(textMeasurer, style, textAndCount) {
        textAndCount.associate { (text, _) ->
            text to textMeasurer.measure(text = text, style = style)
        }
    }

    fun next(): String {
        val breakpoint = random.nextInt(textAndAccumulatedCount.lastOrNull()?.second ?: 0)
        val index = textAndAccumulatedCount.binarySearch {
            it.second - breakpoint
        }
        if (index < 0) {
            return textAndAccumulatedCount[-index - 1].first
        }
        return textAndAccumulatedCount[index].first
    }

    Canvas(modifier) {
        if (textAndCount.isEmpty()) {
            return@Canvas
        }
        val learnInRadian = (learnDegrees * PI / 180).toFloat()
        val hPadding = horizontalSpacing.toPx()
        val vPadding = verticalSpacing.toPx()

        var top = 0f
        while (top < size.height) {
            var left = 0f
            val textsInRow = buildList {
                while (left < this@Canvas.size.width) {
                    val text = next()
                    val layout = layoutByText[text]!!
                    left += layout.size.width
                    add(text)
                }
            }
            left = (size.width - left) / 2
            var maxHeight = 0f
            var maxWidth = 0f
            for (text in textsInRow) {
                val layout = layoutByText[text]!!
                rotate(
                    learnDegrees,
                    Offset(left + layout.size.width / 2, top + layout.size.height / 2)
                ) {
                    drawText(
                        textLayoutResult = layout,
                        color = style.color,
                        topLeft = Offset(left, top)
                    )
                }
                left += cos(learnInRadian) * layout.size.width + sin(learnInRadian) * layout.size.height + hPadding
                if (layout.size.height > maxHeight) {
                    maxHeight = layout.size.height.toFloat()
                }
                if (layout.size.width > maxWidth) {
                    maxWidth = layout.size.width.toFloat()
                }
            }
            top += cos(learnInRadian) * maxHeight + sin(learnInRadian) * maxWidth + vPadding
        }
    }
}


@Preview
@Composable
fun TextCanvasPreview() {
    TextCanvas(
        Modifier.size(400.dp, 400.dp),
        "🤔" to 1,
        "🫠" to 2,
        "☠️" to 3
    )
}