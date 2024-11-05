package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.ImageFrameSkeleton
import com.zhufucdev.practiso.composable.OptionSkeleton
import com.zhufucdev.practiso.composable.OptionsFrameSkeleton
import com.zhufucdev.practiso.composable.TextFrameSkeleton
import com.zhufucdev.practiso.composition.secondaryClickable
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import com.zhufucdev.practiso.viewmodel.QuizViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.add_frame_para
import practiso.composeapp.generated.resources.baseline_elevator_down
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.cat_walker
import practiso.composeapp.generated.resources.confirm_para
import practiso.composeapp.generated.resources.empty_text_para
import practiso.composeapp.generated.resources.expand_para
import practiso.composeapp.generated.resources.frame_type_span
import practiso.composeapp.generated.resources.get_started_by_checking_sheet_para
import practiso.composeapp.generated.resources.image_frame_span
import practiso.composeapp.generated.resources.image_option_para
import practiso.composeapp.generated.resources.navigate_up_para
import practiso.composeapp.generated.resources.new_option_para
import practiso.composeapp.generated.resources.new_options_frame_para
import practiso.composeapp.generated.resources.new_question_para
import practiso.composeapp.generated.resources.options_frame_span
import practiso.composeapp.generated.resources.question_is_empty_para
import practiso.composeapp.generated.resources.question_name_para
import practiso.composeapp.generated.resources.remove_from_keys_span
import practiso.composeapp.generated.resources.remove_para
import practiso.composeapp.generated.resources.rename_para
import practiso.composeapp.generated.resources.sample_image_para
import practiso.composeapp.generated.resources.sample_option_para
import practiso.composeapp.generated.resources.sample_text_para
import practiso.composeapp.generated.resources.set_as_key_span
import practiso.composeapp.generated.resources.text_frame_span
import practiso.composeapp.generated.resources.text_option_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreateApp(
    model: QuizCreateViewModel = viewModel(factory = QuizCreateViewModel.Factory),
    quizViewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val coroutine = rememberCoroutineScope()
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            LargeTopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = quizViewModel.name.takeIf(String::isNotEmpty) ?: stringResource(
                        Res.string.new_question_para
                    ), modifier = Modifier.clickable {
                        model.showNameEditDialog = true
                    })
                }
            }, navigationIcon = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.navigate_up_para)) } },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = {
                            coroutine.launch {
                                Navigator.navigate(Navigation.Backward)
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_up_para)
                        )
                    }
                }
            }, scrollBehavior = topBarScrollBehavior
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
                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.height(300.dp).fillMaxWidth(0.618f),
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

                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> quizViewModel.frames.add(Frame.Text())
                            1 -> quizViewModel.frames.add(Frame.Image())
                            2 -> quizViewModel.frames.add(Frame.Options())
                        }
                        coroutine.launch {
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
        if (quizViewModel.frames.isEmpty()) {
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
                Modifier.fillMaxSize().padding(p).padding(horizontal = PaddingBig)
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
            ) {
                quizViewModel.frames.forEachIndexed { index, frame ->
                    item {
                        when (frame) {
                            is Frame.Image -> TODO()
                            is Frame.Options -> {
                                EditableOptionsFrame(value = frame,
                                    onValueChange = { quizViewModel.frames[index] = it },
                                    onDelete = {
                                        quizViewModel.frames.removeAt(index)
                                    })
                            }

                            is Frame.Text -> {
                                EditableTextFrame(value = frame,
                                    onValueChange = { quizViewModel.frames[index] = it },
                                    onDelete = {
                                        quizViewModel.frames.removeAt(index)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    if (model.showNameEditDialog) {
        QuizNameEditDialog(value = model.nameEditValue,
            onValueChange = { model.nameEditValue = it },
            onDismissRequest = {
                model.showNameEditDialog = false
            },
            onConfirm = {
                quizViewModel.name = model.nameEditValue
                model.showNameEditDialog = false
            },
            onCancel = {
                model.nameEditValue = quizViewModel.name
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

@Composable
private fun GlowingSurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
    glow: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (glow) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier =
        if (onSecondaryClick != null) modifier.secondaryClickable(onSecondaryClick)
        else modifier,
        content = content
    )
}

@Composable
private fun EditableTextFrame(
    value: Frame.Text,
    onValueChange: (Frame.Text) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf(false) }
    var buffer by remember { mutableStateOf(value.textFrame.content) }

    AnimatedContent(!editing) { showTextField ->
        if (showTextField) {
            GlowingSurface(
                onClick = { editing = true },
                onSecondaryClick = { contextMenu = true },
                glow = value.textFrame.content.isEmpty()
            ) {
                TextFrameSkeleton {
                    Text(
                        value.textFrame.content.takeIf(String::isNotEmpty)
                            ?: stringResource(Res.string.empty_text_para),
                    )
                }
                DropdownMenu(expanded = contextMenu, onDismissRequest = { contextMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_para)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = onDelete
                    )
                }
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }

            Column {
                TextField(
                    value = buffer,
                    onValueChange = { buffer = it },
                    placeholder = { Text(stringResource(Res.string.empty_text_para)) },
                    modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingNormal, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        buffer = value.textFrame.content
                        editing = false
                    }) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                    Button(onClick = {
                        onValueChange(value.copy(value.textFrame.copy(content = buffer)))
                        editing = false
                    }) {
                        Text(stringResource(Res.string.confirm_para))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableOptionsFrame(
    value: Frame.Options,
    onValueChange: (Frame.Options) -> Unit,
    onDelete: () -> Unit,
) {
    var frame by remember { mutableStateOf(value.optionsFrame) }
    val options = remember { value.frames.toMutableStateList() }
    var editingName by remember { mutableStateOf(false) }
    var appendingMenu by remember { mutableStateOf(false) }
    var masterMenu by remember { mutableStateOf(false) }

    OptionsFrameSkeleton(
        label = {
            AnimatedContent(editingName) { showTextField ->
                if (!showTextField) {
                    GlowingSurface(
                        onClick = { editingName = true },
                        onSecondaryClick = { masterMenu = true },
                        glow = frame.name.isNullOrEmpty()
                    ) {
                        Text(
                            frame.name?.takeIf(String::isNotEmpty)
                                ?: stringResource(Res.string.new_options_frame_para)
                        )

                        DropdownMenu(
                            expanded = masterMenu,
                            onDismissRequest = { masterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.remove_para)) },
                                onClick = onDelete,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                } else {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(true) {
                        focusRequester.requestFocus()
                    }

                    TextField(
                        value = frame.name ?: "",
                        onValueChange = {
                            frame = frame.copy(name = it.takeIf(String::isNotEmpty))
                        },
                        placeholder = {
                            Text(stringResource(Res.string.new_options_frame_para))
                        },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { editingName = false }
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null)
                            }
                        },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }
        },
        content = {
            options.forEachIndexed { index, option ->
                OptionSkeleton(
                    prefix = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        stringResource(
                                            if (option.isKey) {
                                                Res.string.remove_from_keys_span
                                            } else {
                                                Res.string.set_as_key_span
                                            }
                                        )
                                    )
                                }
                            }
                        ) {
                            Checkbox(
                                checked = option.isKey,
                                onCheckedChange = { options[index] = option.copy(isKey = it) }
                            )
                        }
                    },
                    content = {
                        when (option.frame) {
                            is Frame.Image -> TODO()
                            is Frame.Text -> EditableTextFrame(value = option.frame,
                                onValueChange = { options[index] = option.copy(frame = it) },
                                onDelete = { options.removeAt(index) })

                            else -> throw UnsupportedOperationException("${option.frame::class.simpleName} is not supported")
                        }
                    }
                )
            }

            Box {
                OptionSkeleton(
                    prefix = {
                        Checkbox(
                            checked = false,
                            enabled = false,
                            onCheckedChange = {}
                        )
                    },
                    content = {
                        GlowingSurface(
                            onClick = { appendingMenu = true }
                        ) {
                            Text(stringResource(Res.string.new_option_para))
                        }
                    }
                )
                DropdownMenu(
                    expanded = appendingMenu,
                    onDismissRequest = { appendingMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.text_option_para)) },
                        onClick = {
                            options.add(
                                KeyedPrioritizedFrame(
                                    frame = Frame.Text(),
                                    priority = 0,
                                    isKey = false
                                )
                            )
                            appendingMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.image_option_para)) },
                        onClick = {
                            options.add(
                                KeyedPrioritizedFrame(
                                    frame = Frame.Image(),
                                    priority = 0,
                                    isKey = false
                                )
                            )
                            appendingMenu = false
                        }
                    )
                }
            }
        }
    )
}