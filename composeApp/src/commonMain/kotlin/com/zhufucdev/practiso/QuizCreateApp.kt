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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.EditableImageFrame
import com.zhufucdev.practiso.composable.EditableOptionsFrame
import com.zhufucdev.practiso.composable.EditableTextFrame
import com.zhufucdev.practiso.composable.ImageFrameSkeleton
import com.zhufucdev.practiso.composable.OptionSkeleton
import com.zhufucdev.practiso.composable.OptionsFrameSkeleton
import com.zhufucdev.practiso.composable.TextFrameSkeleton
import com.zhufucdev.practiso.database.AppDatabase
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
import com.zhufucdev.practiso.viewmodel.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.add_frame_para
import practiso.composeapp.generated.resources.baseline_content_save_outline
import practiso.composeapp.generated.resources.baseline_elevator_down
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.cat_walker
import practiso.composeapp.generated.resources.confirm_para
import practiso.composeapp.generated.resources.expand_para
import practiso.composeapp.generated.resources.frame_type_span
import practiso.composeapp.generated.resources.get_started_by_checking_sheet_para
import practiso.composeapp.generated.resources.image_frame_span
import practiso.composeapp.generated.resources.navigate_up_para
import practiso.composeapp.generated.resources.new_question_para
import practiso.composeapp.generated.resources.options_frame_span
import practiso.composeapp.generated.resources.question_is_empty_para
import practiso.composeapp.generated.resources.question_name_para
import practiso.composeapp.generated.resources.rename_para
import practiso.composeapp.generated.resources.sample_image_para
import practiso.composeapp.generated.resources.sample_option_para
import practiso.composeapp.generated.resources.sample_text_para
import practiso.composeapp.generated.resources.save_para
import practiso.composeapp.generated.resources.text_frame_span

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreateApp(
    model: QuizCreateViewModel = viewModel(factory = QuizCreateViewModel.Factory),
    quizViewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val coroutine = rememberCoroutineScope()
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val contentScrollState = rememberLazyListState()
    var idCounter by remember { mutableLongStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    if (saving) {
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
                        Text(text = quizViewModel.name.takeIf(String::isNotEmpty) ?: stringResource(
                            Res.string.new_question_para
                        ), modifier = Modifier.clickable {
                            model.showNameEditDialog = true
                        })
                    }
                },
                navigationIcon = {
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
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(Res.string.save_para)) }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                coroutine.launch {
                                    saving = true
                                    quizViewModel.frames.saveTo(
                                        Database.app,
                                        quizViewModel.name
                                    )
                                    Navigator.navigate(Navigation.Backward)
                                }
                            }
                        ) {
                            if (!saving) {
                                Icon(
                                    painterResource(Res.drawable.baseline_content_save_outline),
                                    contentDescription = null
                                )
                            } else {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
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
                            0 -> quizViewModel.frames.add(
                                Frame.Text(TextFrame(idCounter++, ""))
                            )

                            1 -> quizViewModel.frames.add(
                                Frame.Image(ImageFrame(idCounter++, "", 0, 0, null))
                            )

                            2 -> quizViewModel.frames.add(
                                Frame.Options(OptionsFrame(idCounter++, null))
                            )
                        }
                        coroutine.launch {
                            contentScrollState.scrollToItem(quizViewModel.frames.lastIndex)
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
        AnimatedContent(quizViewModel.frames.isEmpty()) { showHelper ->
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
                    modifier = Modifier.fillMaxSize().padding(p).padding(horizontal = PaddingBig)
                        .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                    state = contentScrollState
                ) {
                    quizViewModel.frames.forEachIndexed { index, frame ->
                        item(frame.id) {
                            when (frame) {
                                is Frame.Image -> {
                                    EditableImageFrame(
                                        value = frame,
                                        onValueChange = { quizViewModel.frames[index] = it },
                                        onDelete = { quizViewModel.frames.removeAt(index) },
                                        modifier = Modifier.animateItem(),
                                        cache = model.imageCache
                                    )
                                }

                                is Frame.Options -> {
                                    EditableOptionsFrame(
                                        value = frame,
                                        onValueChange = { quizViewModel.frames[index] = it },
                                        onDelete = {
                                            quizViewModel.frames.removeAt(index)
                                        },
                                        modifier = Modifier.animateItem(),
                                        imageCache = model.imageCache
                                    )
                                }

                                is Frame.Text -> {
                                    EditableTextFrame(
                                        value = frame,
                                        onValueChange = { quizViewModel.frames[index] = it },
                                        onDelete = {
                                            quizViewModel.frames.removeAt(index)
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

private val Frame.id: Long
    get() = when (this) {
        is Frame.Image -> imageFrame.id
        is Frame.Options -> optionsFrame.id
        is Frame.Text -> textFrame.id
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

private suspend fun List<Frame>.saveTo(db: AppDatabase, name: String) =
    withContext(Dispatchers.IO) {
        db.quizQueries.insertQuiz(name, Clock.System.now(), null)
        val quizId = db.quizQueries.lastInsertRowId().executeAsOne()

        mapIndexed { index, frame ->
            when (frame) {
                is Frame.Text -> async {
                    db.transaction {
                        db.quizQueries.insertTextFrame(frame.textFrame.content)
                        db.quizQueries.associateLastTextFrameWithQuiz(quizId, index.toLong())
                    }
                }

                is Frame.Image -> async {
                    db.transaction {
                        frame.insertTo(db)
                        db.quizQueries.associateLastImageFrameWithQuiz(quizId, index.toLong())
                    }
                }

                is Frame.Options -> async {
                    db.quizQueries.insertOptionsFrame(frame.optionsFrame.name)
                    val frameId = db.quizQueries.lastInsertRowId().executeAsOne()
                    db.quizQueries.associateLastOptionsFrameWithQuiz(quizId, index.toLong())

                    frame.frames.map { optionFrame ->
                        when (optionFrame.frame) {
                            is Frame.Image -> async {
                                db.transaction {
                                    optionFrame.frame.insertTo(db)
                                    db.quizQueries.assoicateLastImageFrameWithOption(
                                        frameId,
                                        maxOf(optionFrame.priority, 0).toLong(),
                                        optionFrame.isKey
                                    )
                                }
                            }

                            is Frame.Text -> async {
                                db.transaction {
                                    db.quizQueries.insertTextFrame(optionFrame.frame.textFrame.content)
                                    db.quizQueries.assoicateLastTextFrameWithOption(
                                        frameId,
                                        optionFrame.isKey,
                                        maxOf(optionFrame.priority, 0).toLong()
                                    )
                                }
                            }

                            is Frame.Options -> throw UnsupportedOperationException("Options frame inception")
                        }
                    }.awaitAll()
                }
            }
        }.awaitAll()
    }

private fun Frame.Image.insertTo(db: AppDatabase) {
    db.quizQueries.insertImageFrame(
        imageFrame.filename,
        imageFrame.altText,
        imageFrame.width,
        imageFrame.height
    )
}