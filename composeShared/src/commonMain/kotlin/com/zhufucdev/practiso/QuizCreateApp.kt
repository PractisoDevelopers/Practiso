package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.EditableImageFrame
import com.zhufucdev.practiso.composable.EditableOptionsFrame
import com.zhufucdev.practiso.composable.EditableTextFrame
import com.zhufucdev.practiso.composable.HorizontalPageIndicator
import com.zhufucdev.practiso.composable.ImageFrameSkeleton
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.composable.OptionSkeleton
import com.zhufucdev.practiso.composable.OptionsFrameSkeleton
import com.zhufucdev.practiso.composable.TextFrameSkeleton
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel.State.NotFound
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel.State.Pending
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel.State.Ready
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.add_frame_para
import resources.baseline_content_save_outline
import resources.baseline_elevator_down
import resources.baseline_redo
import resources.baseline_undo
import resources.cancel_para
import resources.cat_walker
import resources.confirm_para
import resources.expand_para
import resources.frame_type_span
import resources.get_started_by_checking_sheet_para
import resources.image_frame_span
import resources.new_question_para
import resources.options_frame_span
import resources.question_is_empty_para
import resources.question_name_para
import resources.redo_para
import resources.rename_para
import resources.requested_quiz_not_found_para
import resources.sample_image_para
import resources.sample_option_para
import resources.sample_text_para
import resources.save_para
import resources.text_frame_span
import resources.undo_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreateApp(
    model: QuizCreateViewModel = viewModel(factory = QuizCreateViewModel.Factory),
) {
    when (model.state) {
        Ready -> Editor(model)
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = { NavigateUpButton() }
                    )
                }
            ) {
                Box(Modifier.padding(it).fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (model.state == Pending) {
                        CircularProgressIndicator()
                    } else if (model.state == NotFound) {
                        Text(
                            stringResource(Res.string.requested_quiz_not_found_para),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Editor(model: QuizCreateViewModel) {
    val coroutine = rememberCoroutineScope()
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val contentScrollState = rememberLazyListState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    if (model.saving) {
        Popup {
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.5f))
            )
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.name.takeIf(String::isNotEmpty) ?: stringResource(
                                Res.string.new_question_para
                            ), modifier = Modifier.clickable {
                                model.showNameEditDialog = true
                            })
                    }
                },
                navigationIcon = { NavigateUpButton() },
                actions = {
                    IconButtonWithPlainTooltip(
                        onClick = {
                            coroutine.launch {
                                model.event.undo.send(Unit)
                            }
                        },
                        enabled = model.canUndo,
                        tooltipContent = { Text(stringResource(Res.string.undo_para)) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_undo),
                            contentDescription = null
                        )
                    }
                    IconButtonWithPlainTooltip(
                        onClick = {
                            coroutine.launch {
                                model.event.redo.send(Unit)
                            }
                        },
                        enabled = model.canRedo,
                        tooltipContent = { Text(stringResource(Res.string.redo_para)) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_redo),
                            contentDescription = null
                        )
                    }

                    IconButtonWithPlainTooltip(
                        onClick = {
                            coroutine.launch {
                                model.event.save.send(Unit)
                                Navigator.navigate(Navigation.Backward)
                            }
                        },
                        tooltipContent = {
                            Text(stringResource(Res.string.save_para))
                        }
                    ) {
                        if (!model.saving) {
                            Icon(
                                painterResource(Res.drawable.baseline_content_save_outline),
                                contentDescription = null
                            )
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                },
                scrollBehavior = topBarScrollBehavior
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = PaddingBig)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PaddingBig),
            ) {
                val pagerState = rememberPagerState { 3 }
                HorizontalPager(state = pagerState) { page ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.height(300.dp).then(
                                when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
                                    WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED -> Modifier.fillMaxWidth(
                                        0.618f
                                    )

                                    else -> Modifier.fillMaxWidth().padding(horizontal = PaddingBig)
                                }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (page) {
                                0 -> SampleFrameContainer(label = { Text(stringResource(Res.string.text_frame_span)) }) {
                                    Box(Modifier.padding(PaddingNormal)) {
                                        SampleTextFrame()
                                    }
                                }

                                1 -> SampleFrameContainer(
                                    label = { Text(stringResource(Res.string.image_frame_span)) },
                                ) {
                                    Box(
                                        Modifier.padding(PaddingNormal).fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SampleImageFrame()
                                    }
                                }

                                2 -> SampleFrameContainer(label = { Text(stringResource(Res.string.options_frame_span)) }) {
                                    Box(
                                        Modifier.padding(PaddingNormal).fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SampleOptionsFrame()
                                    }
                                }

                                else -> SampleFrameContainer(label = { Text(stringResource(Res.string.frame_type_span)) }) {}
                            }
                        }
                    }
                }

                HorizontalPageIndicator(
                    pageCount = pagerState.pageCount,
                    currentPage = pagerState.currentPage,
                    onSwitch = {
                        coroutine.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    }
                )

                Button(
                    onClick = {
                        coroutine.launch {
                            val id = ++model.lastFrameId
                            when (pagerState.currentPage) {
                                0 -> model.event.add.send(
                                    Frame.Text(id, TextFrame(id, ""))
                                )

                                1 -> model.event.add.send(
                                    Frame.Image(id, ImageFrame(id,"", 0, 0, null))
                                )

                                2 -> model.event.add.send(
                                    Frame.Options(OptionsFrame(id, null))
                                )
                            }
                        }
                        coroutine.launch {
                            contentScrollState.scrollToItem(model.frames.lastIndex)
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = PaddingSmall)
                    )
                    Text(
                        stringResource(Res.string.add_frame_para),
                        Modifier.padding(horizontal = PaddingNormal)
                    )
                }
            }
        },
    ) { p ->
        AnimatedContent(model.frames.isEmpty()) { showHelper ->
            if (showHelper) {
                AlertHelper(
                    header = {
                        Icon(
                            painterResource(Res.drawable.baseline_elevator_down),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )
                    },
                    label = { Text(stringResource(Res.string.question_is_empty_para)) },
                    helper = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(Res.string.get_started_by_checking_sheet_para))
                            Button(onClick = {
                                coroutine.launch {
                                    scaffoldState.bottomSheetState.expand()
                                }
                            }) {
                                Text(stringResource(Res.string.expand_para))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(p)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(p)
                        .padding(horizontal = PaddingBig)
                        .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                    state = contentScrollState
                ) {
                    items(items = model.frames, key = { it::class.simpleName + it.id }) { frame ->
                        when (frame) {
                            is Frame.Image -> {
                                EditableImageFrame(
                                    value = frame,
                                    onValueChange = {
                                        coroutine.launch {
                                            model.event.update.send(it)
                                        }
                                    },
                                    onDelete = {
                                        coroutine.launch {
                                            model.event.remove.send(frame)
                                        }
                                    },
                                    modifier = Modifier.animateItem(),
                                    deleteImageOnRemoval = false,
                                    cache = model.imageCache
                                )
                            }

                            is Frame.Options -> {
                                EditableOptionsFrame(
                                    value = frame,
                                    onValueChange = {
                                        coroutine.launch {
                                            model.event.update.send(it)
                                        }
                                    },
                                    onDelete = {
                                        coroutine.launch {
                                            model.event.remove.send(frame)
                                        }
                                    },
                                    modifier = Modifier.animateItem(),
                                    imageCache = model.imageCache
                                )
                            }

                            is Frame.Text -> {
                                EditableTextFrame(
                                    value = frame,
                                    onValueChange = {
                                        coroutine.launch {
                                            model.event.update.send(it)
                                        }
                                    },
                                    onDelete = {
                                        coroutine.launch {
                                            model.event.remove.send(frame)
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (model.showNameEditDialog) {
        QuizNameEditDialog(
            value = model.nameEditValue,
            onValueChange = { model.nameEditValue = it },
            onDismissRequest = {
                model.showNameEditDialog = false
            },
            onConfirm = {
                coroutine.launch {
                    model.event.rename.send(model.nameEditValue)
                    model.showNameEditDialog = false
                }
            },
            onCancel = {
                model.nameEditValue = model.name
                model.showNameEditDialog = false
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizNameEditDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            shape = AlertDialogDefaults.shape,
            colors = CardDefaults.cardColors(AlertDialogDefaults.containerColor)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingBig),
                modifier = Modifier.padding(PaddingBig)
            ) {
                Text(
                    stringResource(Res.string.rename_para),
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(stringResource(Res.string.new_question_para)) },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.question_name_para)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onCancel) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                    TextButton(onConfirm) {
                        Text(stringResource(Res.string.confirm_para))
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleTextFrame() {
    TextFrameSkeleton {
        Text(
            stringResource(Res.string.sample_text_para),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SampleImageFrame() {
    ImageFrameSkeleton(
        image = {
            Image(
                imageResource(Res.drawable.cat_walker),
                contentDescription = stringResource(Res.string.sample_image_para)
            )
        },
        altText = { Text(stringResource(Res.string.sample_image_para)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SampleOptionsFrame() {
    OptionsFrameSkeleton {
        repeat(4) {
            OptionSkeleton(prefix = {
                RadioButton(
                    selected = false, onClick = null
                )
            }, content = {
                Text(
                    stringResource(Res.string.sample_option_para),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.wrapContentHeight()
                )
            })
        }
    }
}

@Composable
private fun SampleFrameContainer(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingSmall),
        modifier = modifier
    ) {
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            content()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelSmall
        ) {
            label()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconButtonWithPlainTooltip(
    onClick: () -> Unit,
    enabled: Boolean = true,
    tooltipContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip { tooltipContent() }
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            content()
        }
    }
}