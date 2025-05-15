package com.zhufucdev.practiso.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.CommonActions
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.SectionEditScaffold
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.QuizSectionEditVM
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.archive_para
import resources.baseline_archive_arrow_down_outline
import resources.dismiss_para
import resources.remove_para
import resources.would_you_like_to_remove_n_items_para

@Composable
fun QuizSectionEditApp(
    startpoint: QuizSectionEditVM.Startpoint,
    libraryVM: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory),
    model: QuizSectionEditVM = viewModel(factory = QuizSectionEditVM.Factory),
) {
    var deleteDialogShown by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    val items by libraryVM.quiz.collectAsState(emptyList())

    LaunchedEffect(startpoint, model) {
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
                                deleteDialogShown = true
                            },
                            enabled = model.selection.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                },
                floatingActionButton = {
                    PlainTooltipBox(stringResource(Res.string.archive_para)) {
                        FloatingActionButton(
                            onClick = {
                                coroutine.launch {
                                    val archiveFile = FileKit.openFileSaver(
                                        suggestedName = model.describeSelection(),
                                        extension = "psarchive"
                                    )
                                    if (archiveFile != null) {
                                        model.events.exportToFile.send(archiveFile)
                                    }
                                }
                            }
                        ) {
                            Icon(painterResource(Res.drawable.baseline_archive_arrow_down_outline), contentDescription = null)
                        }
                    }
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