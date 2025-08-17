package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.ChipSkeleton
import com.zhufucdev.practiso.composable.DialogWithTextInput
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.QuizSkeleton
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.SomeGroup
import com.zhufucdev.practiso.composable.filter
import com.zhufucdev.practiso.composable.filteredItems
import com.zhufucdev.practiso.composable.rememberFilterController
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.composition.TopLevelDestination
import com.zhufucdev.practiso.composition.combineClickable
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.platform.createOptionView
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_flag_checkered
import resources.cancel_para
import resources.confirm_para
import resources.create_session_para
import resources.finish_para
import resources.head_to_library_to_create
import resources.no_options_available_para
import resources.select_category_to_begin_para
import resources.session_name_para
import resources.stranded_quizzes_para
import kotlin.math.max
import kotlin.math.min

@Composable
fun SessionStarter(
    model: SessionStarterAppViewModel = viewModel(factory = SessionStarterAppViewModel.Factory),
) {
    val items by model.items.collectAsState()
    val coroutine = rememberCoroutineScope()
    AnimatedContent(items?.isEmpty() == true) { empty ->
        if (empty) {
            PlaceHolder(
                header = {
                    Text("🤔")
                },
                label = {
                    Text(stringResource(Res.string.no_options_available_para))
                },
                helper = {
                    Text(stringResource(Res.string.head_to_library_to_create))
                }
            )
        } else {
            val filterController = rememberFilterController(
                items = items ?: emptyList(),
                groupSelector = { it.dimensions }
            )
            val backgroundColor = MaterialTheme.colorScheme.background
            LazyColumn(Modifier.fillMaxSize()) {
                items?.let {
                    filter(
                        Modifier.fillMaxWidth().background(backgroundColor).padding(PaddingNormal),
                        controller = filterController,
                        key = { it.id },
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                        verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                        targetHeightExpanded = min(
                            max(100, filterController.groupedItems.size * 24),
                            200
                        ).dp
                    ) { item ->
                        ChipSkeleton(
                            selected = item in filterController.selectedGroups,
                            label = {
                                Text(
                                    if (item is SomeGroup) {
                                        item.value.name
                                    } else {
                                        stringResource(Res.string.stranded_quizzes_para)
                                    }
                                )
                            },
                            modifier = Modifier.combineClickable(
                                onClick = {
                                    filterController.toggleGroup(item)
                                },
                                onSecondaryClick = {
                                    filterController.toggleGroup(item, true)
                                    if (item !is SomeGroup) {
                                        return@combineClickable
                                    }

                                    coroutine.launch {
                                        if (item.value.id in model.selection.dimensionIds) {
                                            model.event.selectCategory.send(item.value.id)
                                        } else {
                                            model.event.deselectCategory.send(item.value.id)
                                        }
                                    }
                                }
                            )
                        )
                    }
                } ?: stickyHeader {
                    Row(
                        modifier = Modifier.padding(PaddingNormal),
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        repeat(4) {
                            ChipSkeleton()
                        }
                    }
                }
                if (filterController.selectedGroups.isEmpty()) {
                    item {
                        Text(stringResource(Res.string.select_category_to_begin_para))
                    }
                }
                filteredItems(
                    filterController,
                    key = { it.quiz.id },
                    sort = { a, b ->
                        val aSelected = model.isItemSelected(a)
                        val bSelected = model.isItemSelected(b)
                        when {
                            aSelected && !bSelected -> -1
                            !aSelected && bSelected -> 1
                            else -> (a.quiz.id - b.quiz.id).toInt()
                        }
                    }
                ) {
                    QuizItem(
                        modifier = Modifier.animateItem(),
                        option = it.quiz,
                        hasSeparator = true,
                        checked = model.isItemSelected(it),
                        onClick = {
                            coroutine.launch {
                                if (it.quiz.id !in model.selection.quizIds) {
                                    model.event.selectQuiz.send(it.quiz.id)
                                } else {
                                    model.event.deselectQuiz.send(it.quiz.id)
                                }
                            }
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(PaddingSpace))
                }
            }
        }
    }

    SharedElementTransitionPopup(
        key = "finish",
        sharedElement = {
            FabFinish(onClick = {}, modifier = it)
        },
        popup = {
            val navController = LocalNavController.current
            Card(
                shape = FloatingActionButtonDefaults.extendedFabShape,
                modifier = Modifier.pseudoClickable().imePadding()
            ) {
                DialogWithTextInput(
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_flag_checkered),
                            contentDescription = null
                        )
                    },
                    title = {
                        Text(
                            stringResource(Res.string.create_session_para),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    inputValue = model.newSessionName,
                    onInputValueChange = { model.newSessionName = it },
                    singleLine = true,
                    label = { Text(stringResource(Res.string.session_name_para)) },
                    negativeButton = {
                        OutlinedButton(
                            onClick = { coroutine.launch { collapse() } }
                        ) {
                            Text(stringResource(Res.string.cancel_para))
                        }
                    },
                    positiveButton = {
                        Button(
                            enabled = model.newSessionName.isNotBlank(),
                            onClick = {
                                coroutine.launch {
                                    collapse()
                                    model.event.createSession.send(model.newSessionName)
                                    navController?.navigate(TopLevelDestination.Session.name)
                                }
                            }
                        ) {
                            Text(stringResource(Res.string.confirm_para))
                        }
                    }
                )
            }
        }
    ) {
        composeFromBottomUp("fab") {
            AnimatedVisibility(
                visible = model.selection.quizIds.isNotEmpty()
                        || model.selection.dimensionIds.isNotEmpty(),
                enter = scaleIn(transformOrigin = TransformOrigin.Center),
                exit = scaleOut(transformOrigin = TransformOrigin.Center),
                modifier = Modifier.sharedElement(),
            ) {
                FabFinish(
                    onClick = {
                        coroutine.launch { expand() }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizItem(
    option: QuizOption,
    hasSeparator: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        Modifier
            .clickable(onClick = onClick)
                then modifier
    ) {
        Spacer(Modifier.height(PaddingNormal))

        val view = createOptionView(option)
        QuizSkeleton(
            label = { Text(view.title()) },
            preview = {
                Text(
                    view.preview(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            tailingIcon = {
                if (checked) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = null,
                        modifier = Modifier.width(40.dp)
                    )
                }
            },
            modifier = Modifier.padding(horizontal = PaddingNormal)
        )

        Spacer(Modifier.height(PaddingNormal))

        AnimatedVisibility(hasSeparator) {
            Spacer(
                Modifier.fillMaxWidth().padding(start = PaddingNormal)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceBright)
            )
        }
    }
}

@Composable
private fun FabFinish(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.baseline_flag_checkered),
                contentDescription = null
            )
        },
        text = {
            Text(stringResource(Res.string.finish_para))
        },
        modifier = modifier
    )
}