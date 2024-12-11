package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.add_item_to_get_started_para
import practiso.composeapp.generated.resources.baseline_alert_box_outline
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
import practiso.composeapp.generated.resources.library_is_empty_para
import practiso.composeapp.generated.resources.questions_para
import practiso.composeapp.generated.resources.remove_para
import practiso.composeapp.generated.resources.retry_para
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

    val templates by model.templates.collectAsState(null)
    val dimensions by model.dimensions.collectAsState(null)
    val quizzes by model.quiz.collectAsState(null)

    AnimatedContent(templates?.isEmpty() == true && dimensions?.isEmpty() == true && quizzes?.isEmpty() == true) { empty ->
        if (empty) {
            AlertHelper(
                header = { Text("📝") },
                label = { Text(stringResource(Res.string.library_is_empty_para)) },
                helper = { Text(stringResource(Res.string.add_item_to_get_started_para)) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(start = PaddingNormal, top = PaddingNormal)
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
                    id = { "template_" + it.id }
                )

                flatContent(
                    value = dimensions,
                    caption = {
                        SectionCaption(stringResource(Res.string.dimensions_para))
                    },
                    content = {
                        PractisoOptionView(option = it)
                    },
                    id = { "dimension_" + it.dimension.id }
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
                    id = { "quiz_" + it.quiz.id }
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
            PractisoOptionView(option, modifier = modifier)
        }
    )
}

fun <T> LazyListScope.flatContent(
    value: List<T>?,
    caption: @Composable () -> Unit,
    content: @Composable LazyItemScope.(T) -> Unit,
    id: (T) -> Any,
    skeleton: @Composable () -> Unit = { PractisoOptionSkeleton() },
    skeletonsCount: Int = 3,
) {
    if (value?.isEmpty() == true) {
        return
    }

    item {
        caption()
    }

    value?.let { t ->
        t.forEachIndexed { index, v ->
            item(id(v)) {
                content(v)
                if (index < t.lastIndex) {
                    HorizontalSeparator()
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
                                modifier = Modifier.fillMaxWidth()
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