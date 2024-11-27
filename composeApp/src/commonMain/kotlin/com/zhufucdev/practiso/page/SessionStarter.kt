package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.DimensionSkeleton
import com.zhufucdev.practiso.composable.QuizSkeleton
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.PractisoOption
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel.Item
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.head_to_library_to_create
import practiso.composeapp.generated.resources.no_options_available_para
import practiso.composeapp.generated.resources.select_category_to_begin_para
import practiso.composeapp.generated.resources.stranded_quizzes_para

@Composable
fun SessionStarter(
    model: SessionStarterAppViewModel = viewModel(factory = SessionStarterAppViewModel.Factory),
) {
    composeFromBottomUp("fab", null)

    val items: List<Item>? by model.items.collectAsState(null)
    val itemById by remember(items) {
        derivedStateOf { items?.associateBy { it.id } ?: emptyMap() }
    }
    val coroutine = rememberCoroutineScope()
    val currentItems: List<Item>? by remember(model, items) {
        derivedStateOf {
            items?.filter { it.id in model.currentItemIds }
        }
    }
    val quizzes: List<PractisoOption.Quiz>? by remember(currentItems) {
        derivedStateOf {
            currentItems?.takeIf { it.isNotEmpty() }?.flatMap(Item::quizzes)
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
                        Spacer(Modifier.width(PaddingSmall))
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
                                onClick = {
                                    coroutine.launch {
                                        if (currentItems!!.contains(d)) {
                                            model.event.removeCurrentItem.send(d.id)
                                        } else {
                                            model.event.addCurrentItem.send(d.id)
                                        }
                                    }
                                }
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
                }
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
            preview = { Text(option.previewString()) },
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
