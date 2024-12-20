package com.zhufucdev.practiso.page

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.FloatingPopupButton
import com.zhufucdev.practiso.composable.HorizontalControl
import com.zhufucdev.practiso.composable.HorizontalDraggable
import com.zhufucdev.practiso.composable.HorizontalDraggingControlTargetWidth
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.PractisoOption
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.add_item_to_get_started_para
import practiso.composeapp.generated.resources.baseline_alert_box_outline
import practiso.composeapp.generated.resources.baseline_chevron_down
import practiso.composeapp.generated.resources.baseline_import
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.continue_para
import practiso.composeapp.generated.resources.create_para
import practiso.composeapp.generated.resources.dimensions_para
import practiso.composeapp.generated.resources.dismiss_para
import practiso.composeapp.generated.resources.ignore_para
import practiso.composeapp.generated.resources.import_from_practiso_archive_para
import practiso.composeapp.generated.resources.import_para
import practiso.composeapp.generated.resources.importing_x_items_y_done
import practiso.composeapp.generated.resources.keep_para
import practiso.composeapp.generated.resources.library_is_empty_para
import practiso.composeapp.generated.resources.n_questions_are_within_this_dimension_what_to_do_with_it_para
import practiso.composeapp.generated.resources.questions_para
import practiso.composeapp.generated.resources.remove_para
import practiso.composeapp.generated.resources.removing_x_para
import practiso.composeapp.generated.resources.retry_para
import practiso.composeapp.generated.resources.show_more_para
import practiso.composeapp.generated.resources.skip_para
import practiso.composeapp.generated.resources.templates_para
import practiso.composeapp.generated.resources.unarchiving_this_file_ellipsis_para
import practiso.composeapp.generated.resources.will_import_n_items_to_library

@Composable
fun LibraryApp(
    model: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory),
) {
    var showActions by remember {
        mutableStateOf(false)
    }
    val coroutine = rememberCoroutineScope()

    composeFromBottomUp("fab") {
        val pickerLauncher =
            rememberFilePickerLauncher(
                type = PickerType.File(listOf("psarchive")),
                title = stringResource(Res.string.import_from_practiso_archive_para)
            ) { file ->
                if (file == null) {
                    return@rememberFilePickerLauncher
                }

                coroutine.launch {
                    model.event.import.send(file)
                }
            }
        FloatingPopupButton(
            expanded = showActions,
            onExpandedChange = { showActions = it },
            autoCollapse = true
        ) {
            item(
                label = { Text(stringResource(Res.string.import_para)) },
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
                label = { Text(stringResource(Res.string.create_para)) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    coroutine.launch {
                        Navigator.navigate(Navigation.Goto(AppDestination.QuizCreate))
                    }
                }
            )
        }
    }

    val templates by model.templates.collectAsState(null, Dispatchers.IO)
    val dimensions by model.dimensions.collectAsState(null, Dispatchers.IO)
    val quizzes by model.quiz.collectAsState(null, Dispatchers.IO)
    val revealing by model.revealing.collectAsState()

    AnimatedContent(templates?.isEmpty() == true && dimensions?.isEmpty() == true && quizzes?.isEmpty() == true) { empty ->
        if (empty) {
            AlertHelper(
                header = { Text("📝") },
                label = { Text(stringResource(Res.string.library_is_empty_para)) },
                helper = { Text(stringResource(Res.string.add_item_to_get_started_para)) }
            )
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(revealing, templates, quizzes, dimensions) {
                if (templates == null || quizzes == null || dimensions == null) {
                    return@LaunchedEffect
                }

                val offsets = buildList {
                    add(0)
                    add(last() + (templates?.size ?: 0) + 1)
                    add(last() + (dimensions?.size ?: 0) + 1)
                }
                revealing?.let {
                    when (it.type) {
                        LibraryAppViewModel.RevealableType.Dimension -> {
                            val localIndex = dimensions!!.indexOfFirst { d -> d.id == it.id }
                            model.limits[1] = maxOf(model.limits[1], localIndex + 1)
                            listState.animateScrollToItem(offsets[1] + localIndex)
                        }

                        LibraryAppViewModel.RevealableType.Quiz -> {
                            val localIndex = quizzes!!.indexOfFirst { q -> q.id == it.id }
                            model.limits[2] = maxOf(model.limits[2], localIndex + 1)
                            listState.animateScrollToItem(offsets[2] + localIndex)
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
                        PractisoOptionSkeleton(
                            label = { Text(it.name) },
                            preview = {
                                it.description?.let { p ->
                                    Text(p, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        )
                    },
                    limit = model.limits[0],
                    onIncreaseLimit = {
                        model.limits[0] = it
                    },
                    id = { "template_" + it.id },
                )

                flatContent(
                    value = dimensions,
                    caption = {
                        SectionCaption(stringResource(Res.string.dimensions_para))
                    },
                    content = {
                        var removalDialogExpanded by remember { mutableStateOf(false) }
                        ListItem(
                            option = it,
                            onDelete = {
                                if (it.quizCount > 0) {
                                    removalDialogExpanded = true
                                } else {
                                    coroutine.launch {
                                        model.event.removeDimensionKeepQuizzes.send(it.dimension.id)
                                    }
                                }
                            }
                        )
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
                    limit = model.limits[1],
                    onIncreaseLimit = {
                        model.limits[1] = it
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
                            modifier = Modifier.fillMaxWidth().clickable {
                                coroutine.launch {
                                    Navigator.navigate(
                                        Navigation.Goto(AppDestination.QuizCreate),
                                        options = listOf(
                                            NavigationOption.OpenQuiz(it.quiz.id)
                                        )
                                    )
                                }
                            },
                            option = it,
                            onDelete = {
                                coroutine.launch {
                                    model.event.removeQuiz.send(it.quiz.id)
                                }
                            }
                        )
                    },
                    limit = model.limits[2],
                    onIncreaseLimit = {
                        model.limits[2] = it
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

    val importState by model.importState.collectAsState()
    if (importState !is LibraryAppViewModel.ImportState.Idle) {
        ImportDialog(importState)
    }
}

@Composable
private fun LazyItemScope.ListItem(
    modifier: Modifier = Modifier,
    swipable: Boolean = true,
    option: PractisoOption,
    onDelete: () -> Unit,
) {
    HorizontalDraggable(
        modifier = Modifier.animateItem(),
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
                        foreground, Offset.Zero, size,
                        alpha = 0.5f, blendMode = BlendMode.Lighten
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
private fun ImportDialog(state: LibraryAppViewModel.ImportState) {
    val coroutine = rememberCoroutineScope()
    BasicAlertDialog(
        onDismissRequest = {},
    ) {
        Card {
            if (state !is LibraryAppViewModel.ImportState.Error) {
                DialogContentSkeleton(
                    modifier = Modifier.fillMaxWidth().padding(PaddingBig),
                    icon = {
                        Icon(
                            painterResource(Res.drawable.baseline_import),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            stringResource(Res.string.import_from_practiso_archive_para),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                ) {
                    when (state) {
                        is LibraryAppViewModel.ImportState.Confirmation -> {
                            Text(
                                pluralStringResource(
                                    Res.plurals.will_import_n_items_to_library,
                                    state.total,
                                    state.total
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        is LibraryAppViewModel.ImportState.Importing -> {
                            Text(
                                pluralStringResource(
                                    Res.plurals.importing_x_items_y_done,
                                    state.total,
                                    state.total,
                                    state.done
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is LibraryAppViewModel.ImportState.Unarchiving -> {
                            Text(
                                stringResource(Res.string.unarchiving_this_file_ellipsis_para),
                            )
                        }

                        is LibraryAppViewModel.ImportState.Error,
                        LibraryAppViewModel.ImportState.Idle,
                            -> error("Should never reach here")
                    }
                }
                if (state is LibraryAppViewModel.ImportState.Confirmation) {
                    Column {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.ok.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.continue_para))
                        }
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.dismiss.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.dismiss_para))
                        }
                    }
                }
            } else {
                val error = state.model
                DialogContentSkeleton(
                    modifier = Modifier.fillMaxWidth().padding(PaddingBig),
                    icon = {
                        Icon(
                            painterResource(Res.drawable.baseline_alert_box_outline),
                            contentDescription = null
                        )
                    },
                    title = {
                        Text(error.stringTitle())
                    }
                ) {
                    Text(error.stringContent())
                }
                Column {
                    if (state.skip != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.skip.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.skip_para))
                        }
                    }
                    if (state.ignore != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.ignore.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.ignore_para))
                        }
                    }
                    if (state.retry != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.retry.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.retry_para))
                        }
                    }
                    PrimaryButton(
                        onClick = {
                            coroutine.launch {
                                state.cancel.send(Unit)
                            }
                        },
                    ) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    HorizontalSeparator(Modifier.height(2.dp))
    TextButton(
        onClick = onClick,
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimensionRemovalDialog(
    target: PractisoOption.Dimension,
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