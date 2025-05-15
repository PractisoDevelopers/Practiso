@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.CommonActions
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.SectionEditScaffold
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.viewmodel.DimensionSectionEditVM
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_flag_checkered
import resources.cancel_para
import resources.keep_para
import resources.n_questions_within_x_what_to_do_with_them
import resources.new_take_para
import resources.remove_para
import resources.removing_n_items_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimensionSectionEditApp(
    startpoint: DimensionSectionEditVM.Startpoint,
    libraryVm: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory),
    model: DimensionSectionEditVM = viewModel(factory = DimensionSectionEditVM.Factory),
) {
    val items by libraryVm.dimensions.collectAsState(emptyList())
    val coroutine = rememberCoroutineScope()
    var deleteDialogState by remember { mutableStateOf<DeleteDialogState>(DeleteDialogState.Hidden) }

    LaunchedEffect(model) {
        model.loadStartpoint(startpoint)
    }

    SectionEditScaffold(
        items,
        initialTopItemIndex = maxOf(startpoint.topItemIndex, 0),
        model = model,
        bottomBar = {
            BottomAppBar(
                actions = {
                    CommonActions(model, items)
                    PlainTooltipBox(stringResource(Res.string.remove_para)) {
                        IconButton(
                            onClick = {
                                coroutine.launch {
                                    val quizCount = withContext(Dispatchers.IO) {
                                        model.getDistinctQuizCountInSelection()
                                    }
                                    if (quizCount > 0) {
                                        deleteDialogState = DeleteDialogState.Shown(
                                            quizCount,
                                            model.describeSelection()
                                        )
                                    } else {
                                        model.events.removeKeepQuizzes.send(Unit)
                                    }
                                }
                            },
                            enabled = model.selection.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = model.selection.isNotEmpty(),
                        enter = scaleIn(tween()),
                        exit = scaleOut()
                    ) {
                        PlainTooltipBox(stringResource(Res.string.new_take_para)) {
                            FloatingActionButton(
                                onClick = {
                                    coroutine.launch {
                                        model.events.newTakeFromSelection.send(Unit)
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(Res.drawable.baseline_flag_checkered),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            )
        }
    )

    when (val state = deleteDialogState) {
        is DeleteDialogState.Shown -> {
            BasicAlertDialog(
                onDismissRequest = { deleteDialogState = DeleteDialogState.Hidden }
            ) {
                Card {
                    DialogContentSkeleton(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        title = {
                            Text(
                                pluralStringResource(
                                    Res.plurals.removing_n_items_para,
                                    model.selection.size,
                                    model.selection.size
                                )
                            )
                        },
                        modifier = Modifier.padding(PaddingBig)
                    ) {
                        Text(
                            pluralStringResource(
                                Res.plurals.n_questions_within_x_what_to_do_with_them,
                                state.quizCount,
                                state.quizCount,
                                state.selectionDescription
                            )
                        )
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                deleteDialogState = DeleteDialogState.Hidden
                            }) {
                                Text(stringResource(Res.string.cancel_para))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    coroutine.launch {
                                        model.events.removeWithQuizzes.send(Unit)
                                        deleteDialogState = DeleteDialogState.Hidden
                                    }
                                }
                            ) {
                                Text(stringResource(Res.string.remove_para))
                            }
                            TextButton(
                                onClick = {
                                    coroutine.launch {
                                        model.events.removeKeepQuizzes.send(Unit)
                                        deleteDialogState = DeleteDialogState.Hidden
                                    }
                                }
                            ) {
                                Text(stringResource(Res.string.keep_para))
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}

private sealed class DeleteDialogState {
    object Hidden : DeleteDialogState()
    data class Shown(val quizCount: Int, val selectionDescription: String) : DeleteDialogState()
}