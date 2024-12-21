package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.TopLevelDestination
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.DimensionSkeleton
import com.zhufucdev.practiso.composable.QuizSkeleton
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.composition.combineClickable
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel.Item
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_flag_checkered
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.confirm_para
import practiso.composeapp.generated.resources.create_session_para
import practiso.composeapp.generated.resources.finish_para
import practiso.composeapp.generated.resources.head_to_library_to_create
import practiso.composeapp.generated.resources.no_options_available_para
import practiso.composeapp.generated.resources.select_category_to_begin_para
import practiso.composeapp.generated.resources.session_name_para
import practiso.composeapp.generated.resources.stranded_quizzes_para

@Composable
fun SessionStarter(
    model: SessionStarterAppViewModel = viewModel(factory = SessionStarterAppViewModel.Factory),
) {
    val items: List<Item>? by model.items.collectAsState()
    val itemById by remember(items) {
        derivedStateOf { items?.associateBy { it.id } ?: emptyMap() }
    }
    val coroutine = rememberCoroutineScope()
    val currentItems: List<Item>? by remember(model, items) {
        derivedStateOf {
            items?.filter { it.id in model.currentItemIds }
        }
    }
    val quizzes: Set<PractisoOption.Quiz>? by remember(currentItems) {
        derivedStateOf {
            currentItems?.takeIf { it.isNotEmpty() }?.flatMap(Item::quizzes)?.toSet()
        }
    }
    val selectedQuizzes: List<PractisoOption.Quiz>? by remember(model.selection, quizzes) {
        derivedStateOf {
            quizzes?.filter {
                it.quiz.id in model.selection.quizIds || it in model.selection.dimensionIds.flatMap { i ->
                    itemById[i]?.quizzes ?: emptyList()
                }
            }
        }
    }
    val unselectedQuizzes: List<PractisoOption.Quiz>? by remember(model.selection, quizzes) {
        derivedStateOf {
            quizzes?.filter {
                it.quiz.id !in model.selection.quizIds && it !in model.selection.dimensionIds.flatMap { i ->
                    itemById[i]?.quizzes ?: emptyList()
                }
            }
        }
    }

    LaunchedEffect(items) {
        items?.let {
            it.firstOrNull()?.let { firstItem ->
                model.event.addCurrentItem.send(firstItem.id)
            }
        }
    }

    AnimatedContent(items?.isEmpty() == true) { empty ->
        if (empty) {
            AlertHelper(
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
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(PaddingNormal))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PaddingNormal)) {
                    item("start_spacer") {
                        Spacer(Modifier.width(0.dp))
                    }
                    items?.let {
                        items(it, key = { d -> d.id }) { d ->
                            DimensionSkeleton(
                                selected = currentItems!!.contains(d),
                                label = {
                                    Text(
                                        if (d is Item.Categorized) {
                                            d.dimension.name
                                        } else {
                                            stringResource(Res.string.stranded_quizzes_para)
                                        }
                                    )
                                },
                                modifier = Modifier.combineClickable(
                                    onClick = {
                                        coroutine.launch {
                                            if (currentItems!!.contains(d)) {
                                                model.event.removeCurrentItem.send(d.id)
                                            } else {
                                                model.event.addCurrentItem.send(d.id)
                                            }
                                        }
                                    },
                                    onSecondaryClick = {
                                        coroutine.launch {
                                            if (model.selection.dimensionIds.contains(d.id)) {
                                                model.event.deselectCategory.send(d.id)
                                            } else {
                                                model.event.selectCategory.send(d.id)
                                            }
                                            model.event.addCurrentItem.send(d.id)
                                        }
                                    }
                                )
                            )
                        }
                    } ?: items(4) {
                        DimensionSkeleton()
                    }
                    item("end_spacer") {
                        Spacer(Modifier.width(PaddingSmall))
                    }
                }
                Spacer(Modifier.height(PaddingNormal))
                LazyColumn {
                    selectedQuizzes?.let {
                        it.forEachIndexed { index, option ->
                            item(option.quiz.id) {
                                QuizItem(
                                    option,
                                    hasSeparator = index < it.lastIndex || unselectedQuizzes!!.isNotEmpty(),
                                    checked = true,
                                    onClick = {
                                        coroutine.launch {
                                            model.event.deselectQuiz.send(option.quiz.id)
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    } ?: items?.let { _ ->
                        item {
                            Text(
                                stringResource(Res.string.select_category_to_begin_para),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = PaddingNormal)
                                    .animateItem()
                            )
                        }
                    } ?: items(8) { index ->
                        Spacer(Modifier.height(PaddingNormal))
                        QuizSkeleton(modifier = Modifier.padding(horizontal = PaddingNormal))
                        Spacer(Modifier.height(PaddingNormal))
                        if (index < 7) {
                            Spacer(
                                Modifier.fillMaxWidth().padding(start = PaddingNormal)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceBright)
                            )
                        }
                    }

                    unselectedQuizzes?.let {
                        it.forEachIndexed { index, option ->
                            item(option.quiz.id) {
                                QuizItem(
                                    option,
                                    hasSeparator = index < it.lastIndex,
                                    checked = false,
                                    onClick = {
                                        coroutine.launch {
                                            model.event.selectQuiz.send(option.quiz.id)
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }

                    item("space") {
                        Spacer(Modifier.height(PaddingSpace))
                    }
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
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
            ) {
                DialogContentSkeleton(
                    modifier = Modifier.padding(PaddingBig).fillMaxWidth(),
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
                    }
                ) {
                    OutlinedTextField(
                        value = model.newSessionName,
                        onValueChange = { model.newSessionName = it },
                        label = { Text(stringResource(Res.string.session_name_para)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { coroutine.launch { collapse() } }
                        ) {
                            Text(stringResource(Res.string.cancel_para))
                        }
                        Button(
                            enabled = model.newSessionName.isNotEmpty(),
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
                }
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
    option: PractisoOption.Quiz,
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

        QuizSkeleton(
            label = { Text(option.titleString()) },
            preview = {
                Text(
                    option.previewString(),
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