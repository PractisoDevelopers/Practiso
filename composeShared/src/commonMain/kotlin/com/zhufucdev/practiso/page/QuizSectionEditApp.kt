package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.zhufucdev.practiso.composable.ArchiveSharingDialog
import com.zhufucdev.practiso.composable.CommonActions
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.SectionEditScaffold
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.viewmodel.ArchiveSharingViewModel
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.QuizSectionEditVM
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.dismiss_para
import resources.remove_para
import resources.share_para
import resources.would_you_like_to_remove_n_items_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSectionEditApp(
    startpoint: QuizSectionEditVM.Startpoint,
    libraryVM: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory),
    archiveSharingVM: ArchiveSharingViewModel = viewModel(factory = ArchiveSharingViewModel.Factory),
    model: QuizSectionEditVM = viewModel(factory = QuizSectionEditVM.Factory),
) {
    var deleteDialogShown by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    val items by libraryVM.quiz.collectAsState()

    LaunchedEffect(startpoint, model) {
        model.loadStartpoint(startpoint)
    }

    SectionEditScaffold(
        items ?: emptyList(),
        initialTopItemIndex = maxOf(startpoint.topItemIndex, 0),
        model = model,
        bottomBar = {
            BottomAppBar(
                actions = {
                    CommonActions(model, items ?: emptyList())
                    PlainTooltipBox(stringResource(Res.string.remove_para)) {
                        IconButton(
                            onClick = {
                                deleteDialogShown = true
                            },
                            enabled = model.selection.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                },
                floatingActionButton = {
                    SharedElementTransitionPopup(
                        key = "share",
                        popup = {
                            ArchiveSharingDialog(
                                model = archiveSharingVM,
                                onDismissRequested = {
                                    coroutine.launch {
                                        collapse()
                                    }
                                }
                            )
                        },
                        sharedElement = {
                            FloatingActionButton(onClick = {}, modifier = it) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null
                                )
                            }
                        },
                        content = {
                            AnimatedVisibility(
                                visible = model.selection.isNotEmpty(),
                                enter = scaleIn(tween()),
                                exit = scaleOut(),
                                modifier = Modifier.sharedElement()
                            ) {
                                PlainTooltipBox(stringResource(Res.string.share_para)) {
                                    FloatingActionButton(
                                        onClick = {
                                            archiveSharingVM.loadParameters(selection = model.selection)
                                            coroutine.launch {
                                                expand()
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                }
                            }
                        }
                    )
                }
            )
        }
    )

    if (deleteDialogShown) {
        AlertDialog(
            onDismissRequest = { deleteDialogShown = false },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        coroutine.launch {
                            model.events.removeSection.send(Unit)
                            deleteDialogShown = false
                        }
                    }
                ) {
                    Text(stringResource(Res.string.remove_para))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        deleteDialogShown = false
                    }
                ) {
                    Text(stringResource(Res.string.dismiss_para))
                }
            },
            text = {
                Text(
                    pluralStringResource(
                        Res.plurals.would_you_like_to_remove_n_items_para,
                        model.selection.size,
                        model.selection.size
                    )
                )
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}