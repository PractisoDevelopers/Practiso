package com.zhufucdev.practiso.page

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.Backdrop
import com.zhufucdev.practiso.composable.BackdropKey
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.FloatingPopupButton
import com.zhufucdev.practiso.composable.HorizontalControl
import com.zhufucdev.practiso.composable.HorizontalDraggable
import com.zhufucdev.practiso.composable.HorizontalDraggingControlTargetWidth
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedHorizontalDraggableExclusion
import com.zhufucdev.practiso.composition.combineClickable
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.composition.currentSharedImportViewModel
import com.zhufucdev.practiso.datamodel.DimensionOption
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.DimensionSectionEditVM
import com.zhufucdev.practiso.viewmodel.DimensionViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.QuizSectionEditVM
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.add_item_to_get_started_para
import resources.baseline_axis_arrow
import resources.baseline_chevron_down
import resources.baseline_import
import resources.baseline_timelapse
import resources.cancel_para
import resources.create_dimension_para
import resources.create_dimension_title
import resources.create_para
import resources.create_quiz_title
import resources.dimension_name
import resources.dimensions_para
import resources.import_archive_title
import resources.import_from_practiso_archive_para
import resources.keep_para
import resources.library_is_empty_para
import resources.n_questions_are_within_this_dimension_what_to_do_with_it_para
import resources.new_take_from_x_para
import resources.questions_para
import resources.remove_para
import resources.removing_x_para
import resources.select_para
import resources.show_more_para
import resources.templates_para
import kotlin.math.max

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryApp(
    model: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory),
    importer: ImportViewModel = currentSharedImportViewModel(),
) {
    var showActions by remember { mutableStateOf(false) }
    var showDimCreationDialog by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()

    composeFromBottomUp("fab") {
        val pickerLauncher =
            rememberFilePickerLauncher(
                type = FileKitType.File(setOf("psarchive")),
                title = stringResource(Res.string.import_from_practiso_archive_para)
            ) { file ->
                if (file == null) {
                    return@rememberFilePickerLauncher
                }

                coroutine.launch {
                    importer.event.import.send(NamedSource.fromFile(file))
                }
            }
        FloatingPopupButton(
            expanded = showActions,
            onExpandedChange = {
                showActions = it
                if (showActions) {
                    coroutine.launch {
                        SharedHorizontalDraggableExclusion.current.emit(null)
                    }
                }
            },
            autoCollapse = true
        ) {
            item(
                label = { Text(stringResource(Res.string.import_archive_title)) },
                icon = {
                    Icon(
                        painterResource(Res.drawable.baseline_import),
                        contentDescription = null
                    )
                },
                onClick = {
                    pickerLauncher.launch()
                }
            )
            item(
                label = { Text(stringResource(Res.string.create_dimension_title)) },
                icon = {
                    Icon(
                        painterResource(Res.drawable.baseline_axis_arrow),
                        contentDescription = null
                    )
                },
                onClick = { showDimCreationDialog = true }
            )
            item(
                label = { Text(stringResource(Res.string.create_quiz_title)) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    coroutine.launch {
                        Navigator.navigate(Navigation.Goto(AppDestination.QuizCreate))
                    }
                }
            )
        }
    }

    composeFromBottomUp(BackdropKey) {
        var bounds by remember { mutableStateOf(Rect.Zero) }
        val radius by animateFloatAsState(if (showActions) max(bounds.maxDimension, 1000f) else 1f)
        if (showActions || radius > 1f) {
            Backdrop(
                Modifier.onGloballyPositioned {
                    bounds = it.boundsInParent()
                }.background(
                    brush = Brush.radialGradient(
                        0.9f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        0.95f to MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        1f to Color.Transparent,
                        center = if (LocalLayoutDirection.current == LayoutDirection.Ltr) Offset(
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY
                        )
                        else Offset(0f, Float.POSITIVE_INFINITY),
                        radius = radius
                    )
                )
            ) {
                showActions = false
            }
        }
    }

    val navController = currentNavController()
    val templates by model.templates.collectAsState(null)
    val dimensions by model.dimensions.collectAsState(null)
    val quizzes by model.quiz.collectAsState(null)
    val revealing by model.revealing.collectAsState()
    val offsets = buildList {
        add(0)
        add(last() + (templates?.size ?: 0) + 1)
        add(last() + (dimensions?.size ?: 0) + 1)
    }

    AnimatedContent(templates?.isEmpty() == true && dimensions?.isEmpty() == true && quizzes?.isEmpty() == true) { empty ->
        if (empty) {
            PlaceHolder(
                header = { Text("📝") },
                label = { Text(stringResource(Res.string.library_is_empty_para)) },
                helper = { Text(stringResource(Res.string.add_item_to_get_started_para)) }
            )
        } else {
            val listState = rememberLazyListState()
            val caps by model.caps.collectAsState()

            LaunchedEffect(revealing, templates, quizzes, dimensions) {
                if (templates == null || quizzes == null || dimensions == null) {
                    return@LaunchedEffect
                }

                revealing?.let {
                    when (it.type) {
                        LibraryAppViewModel.RevealableType.Dimension -> {
                            coroutine.launch {
                                val localIndex = dimensions!!.indexOfFirst { d -> d.id == it.id }
                                model.event.updateCaps.send(LibraryAppViewModel.Caps(dimension = localIndex + 1))
                                listState.animateScrollToItem(offsets[1] + localIndex)
                            }
                        }

                        LibraryAppViewModel.RevealableType.Quiz -> {
                            coroutine.launch {
                                val localIndex = quizzes!!.indexOfFirst { q -> q.id == it.id }
                                model.event.updateCaps.send(LibraryAppViewModel.Caps(quiz = localIndex + 1))
                                listState.animateScrollToItem(offsets[2] + localIndex)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = PaddingNormal)
                    .fillMaxWidth(),
            ) {
                flatContent(
                    value = templates,
                    caption = {
                        SectionCaption(stringResource(Res.string.templates_para))
                    },
                    content = {
                        Box {
                            ListItem(
                                option = it,
                                lazyItemScope = this@flatContent,
                                onDelete = {
                                    TODO()
                                },
                            )
                        }
                    },
                    limit = caps.template,
                    onIncreaseLimit = {
                        coroutine.launch {
                            model.event.updateCaps.send(LibraryAppViewModel.Caps(template = it))
                        }
                    },
                    id = { "template_" + it.id },
                )

                flatContent(
                    value = dimensions,
                    caption = {
                        SectionCaption(pluralStringResource(Res.plurals.dimensions_para, dimensions?.size ?: 0))
                    },
                    content = {
                        var removalDialogExpanded by remember { mutableStateOf(false) }
                        var menuExpanded by remember { mutableStateOf(false) }
                        val hapticFeedback = LocalHapticFeedback.current
                        Box {
                            ListItem(
                                lazyItemScope = this@flatContent,
                                option = it,
                                onDelete = {
                                    if (it.quizCount > 0) {
                                        removalDialogExpanded = true
                                    } else {
                                        coroutine.launch {
                                            model.event.removeDimensionKeepQuizzes.send(it.dimension.id)
                                        }
                                    }
                                },
                                modifier =
                                    Modifier.combineClickable(
                                        onClick = {
                                            navController.navigate(
                                                DimensionViewModel.Initialization(
                                                    it.id
                                                )
                                            )
                                        },
                                        onSecondaryClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuExpanded = true
                                        }
                                    )
                            )

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painterResource(Res.drawable.baseline_timelapse),
                                            contentDescription = null
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(
                                                Res.string.new_take_from_x_para,
                                                it.dimension.name
                                            )
                                        )
                                    },
                                    onClick = {
                                        coroutine.launch {
                                            model.event.newTakeFromDimension.send(it.id)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    },
                                    text = {
                                        Text(stringResource(Res.string.select_para))
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        listState.firstVisibleItemScrollOffset
                                        navController.navigate(
                                            DimensionSectionEditVM.Startpoint(
                                                it.dimension.id,
                                                listState.firstVisibleItemIndex - offsets[1]
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        if (removalDialogExpanded) {
                            DimensionRemovalDialog(
                                target = it,
                                onDeleteAll = {
                                    coroutine.launch {
                                        model.event.removeDimensionWithQuizzes.send(it.dimension.id)
                                    }
                                },
                                onDeleteSelfOnly = {
                                    coroutine.launch {
                                        model.event.removeDimensionKeepQuizzes.send(it.dimension.id)
                                    }
                                },
                                onDismiss = {
                                    removalDialogExpanded = false
                                }
                            )
                        }
                    },
                    limit = caps.dimension,
                    onIncreaseLimit = {
                        coroutine.launch {
                            model.event.updateCaps.send(LibraryAppViewModel.Caps(dimension = it))
                        }
                    },
                    id = { "dimension_" + it.dimension.id },
                    revealingIndex =
                        revealing
                            ?.takeIf { it.type == LibraryAppViewModel.RevealableType.Dimension }
                            ?.let { r -> dimensions?.indexOfFirst { it.id == r.id } }
                )

                flatContent(
                    value = quizzes,
                    caption = {
                        SectionCaption(stringResource(Res.string.questions_para))
                    },
                    content = {
                        ListItem(
                            modifier =
                                Modifier.fillMaxWidth().combineClickable(onClick = {
                                    coroutine.launch {
                                        Navigator.navigate(
                                            Navigation.Goto(AppDestination.QuizCreate),
                                            NavigationOption.OpenQuiz(it.quiz.id)
                                        )
                                    }
                                }, onSecondaryClick = {
                                    navController.navigate(
                                        QuizSectionEditVM.Startpoint(
                                            it.quiz.id,
                                            listState.firstVisibleItemIndex - offsets[2]
                                        )
                                    )
                                }),
                            lazyItemScope = this@flatContent,
                            option = it,
                            onDelete = {
                                coroutine.launch {
                                    model.event.removeQuiz.send(it.quiz.id)
                                }
                            }
                        )
                    },
                    limit = caps.quiz,
                    onIncreaseLimit = {
                        coroutine.launch {
                            model.event.updateCaps.send(LibraryAppViewModel.Caps(quiz = it))
                        }
                    },
                    id = { "quiz_" + it.quiz.id },
                    revealingIndex =
                        revealing
                            ?.takeIf { it.type == LibraryAppViewModel.RevealableType.Quiz }
                            ?.let { r -> quizzes?.indexOfFirst { it.id == r.id } }
                )

                item("space") {
                    Spacer(Modifier.height(PaddingSpace))
                }
            }
        }
    }

    if (showDimCreationDialog) {
        DimensionCreationDialog(
            onDismiss = { showDimCreationDialog = false },
            onCreate = {
                showDimCreationDialog = false
                coroutine.launch {
                    model.event.newDimension.send(it)
                }
            }
        )
    }
}

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    lazyItemScope: LazyItemScope,
    swipable: Boolean = true,
    option: PractisoOption,
    onDelete: () -> Unit,
) {
    HorizontalDraggable(
        modifier = with(lazyItemScope) { Modifier.animateItem() },
        enabled = swipable,
        targetWidth = HorizontalDraggingControlTargetWidth + PaddingSmall * 2,
        controls = {
            HorizontalControl(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.clickable(onClick = onDelete, enabled = swipable)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.remove_para)
                )
            }
        },
        content = {
            Box(modifier) {
                PractisoOptionView(option, modifier = Modifier.padding(PaddingNormal))
            }
        }
    )
}

fun <T> LazyListScope.flatContent(
    value: List<T>?,
    caption: @Composable () -> Unit,
    content: @Composable LazyItemScope.(T) -> Unit,
    id: (T) -> Any,
    skeleton: @Composable () -> Unit = {
        PractisoOptionSkeleton(modifier = Modifier.padding(PaddingNormal))
    },
    limit: Int,
    onIncreaseLimit: (Int) -> Unit,
    skeletonsCount: Int = 5,
    revealingIndex: Int? = null,
) {
    if (value?.isEmpty() == true) {
        return
    }

    item {
        Box(Modifier.padding(start = PaddingNormal)) {
            caption()
        }
    }

    value?.let { t ->
        val hasShowMoreItem = limit < t.size
        t.subList(0, minOf(limit, t.size)).forEachIndexed { index, v ->
            item(id(v)) {
                val animator = remember { Animatable(Color.Transparent) }
                val foreground by animator.asState()
                val highlightColor = MaterialTheme.colorScheme.primaryContainer
                LaunchedEffect(v, index, revealingIndex) {
                    if (index == revealingIndex) {
                        repeat(3) {
                            animator.animateTo(highlightColor)
                            animator.animateTo(Color.Transparent)
                        }
                    }
                }
                Box(Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        foreground, Offset.Zero, size, alpha = 0.5f,
                        blendMode = BlendMode.ColorBurn
                    )
                }) {
                    content(v)
                }
                if (index < t.lastIndex || hasShowMoreItem) {
                    Box(Modifier.padding(start = PaddingNormal)) {
                        HorizontalSeparator()
                    }
                }
            }
        }

        if (hasShowMoreItem) {
            item {
                Box(
                    Modifier.fillMaxWidth()
                        .clickable { onIncreaseLimit(limit * 2) }) {
                    Row(Modifier.padding(PaddingNormal)) {
                        Icon(
                            painterResource(Res.drawable.baseline_chevron_down),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(PaddingNormal))
                        Text(stringResource(Res.string.show_more_para))
                    }
                }
            }
        }
    } ?: items(skeletonsCount) { i ->
        skeleton()
        if (i < skeletonsCount - 1) {
            HorizontalSeparator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimensionRemovalDialog(
    target: DimensionOption,
    onDeleteAll: () -> Unit,
    onDeleteSelfOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismiss) {
        Card {
            Column {
                DialogContentSkeleton(
                    modifier = Modifier.padding(PaddingBig),
                    icon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                    title = {
                        Text(stringResource(Res.string.removing_x_para, target.dimension.name))
                    }
                ) {
                    Text(
                        pluralStringResource(
                            Res.plurals.n_questions_are_within_this_dimension_what_to_do_with_it_para,
                            target.quizCount,
                            target.quizCount
                        )
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = PaddingNormal)
                        .padding(bottom = PaddingNormal)
                ) {
                    TextButton(onDismiss) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onDeleteAll) {
                        Text(stringResource(Res.string.remove_para))
                    }
                    TextButton(onDeleteSelfOnly) {
                        Text(stringResource(Res.string.keep_para))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimensionCreationDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var nameBuffer by remember { mutableStateOf("") }
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card {
            DialogContentSkeleton(
                icon = {
                    Icon(
                        painterResource(Res.drawable.baseline_axis_arrow),
                        contentDescription = null
                    )
                },
                title = {
                    Text(
                        stringResource(Res.string.create_dimension_para),
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.padding(PaddingBig)
            ) {
                OutlinedTextField(
                    value = nameBuffer,
                    onValueChange = { nameBuffer = it },
                    label = { Text(stringResource(Res.string.dimension_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                )
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                onCreate(nameBuffer)
                            },
                            enabled = nameBuffer.isNotBlank(),
                        ) {
                            Text(stringResource(Res.string.create_para))
                        }
                    }
                }
            }
        }
    }
}
