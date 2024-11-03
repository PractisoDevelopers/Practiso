package com.zhufucdev.practiso.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import com.zhufucdev.practiso.viewmodel.QuizViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.confirm_para
import practiso.composeapp.generated.resources.navigate_up_para
import practiso.composeapp.generated.resources.new_question_para
import practiso.composeapp.generated.resources.question_name_para
import practiso.composeapp.generated.resources.rename_para

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
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quizViewModel.name.takeIf(String::isNotEmpty)
                                ?: stringResource(Res.string.new_question_para),
                            modifier = Modifier.clickable {
                                model.showNameEditDialog = true
                            }
                        )
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
                scrollBehavior = topBarScrollBehavior
            )
        },
        sheetContent = {

        }
    ) {
        LazyColumn(Modifier.padding(it).nestedScroll(topBarScrollBehavior.nestedScrollConnection)) {

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
                quizViewModel.name = model.nameEditValue
                model.showNameEditDialog = false
            },
            onCancel = {
                model.nameEditValue = quizViewModel.name
                model.showNameEditDialog = false
            }
        )
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
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
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