package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel.Item
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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

    val items by model.items.collectAsState(null)
    val coroutine = rememberCoroutineScope()
    val currentItem: Item? by remember(model, items) {
        derivedStateOf {
            items?.firstOrNull { it.id == model.currentItemId }
        }
    }

    LaunchedEffect(items) {
        items?.let {
            it.firstOrNull()?.let { firstItem ->
                model.event.changeCurrentItem.send(firstItem.id)
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
                                selected = currentItem == d,
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
                                        if (currentItem == d) {
                                            model.event.changeCurrentItem.send(-1)
                                        } else {
                                            model.event.changeCurrentItem.send(d.id)
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingNormal)) {
                    currentItem?.quizzes?.let {
                        it.forEachIndexed { index, quiz ->
                            item(quiz.quiz.id) {
                                QuizSkeleton(
                                    label = { Text(quiz.titleString()) },
                                    preview = { Text(quiz.previewString()) },
                                    modifier = Modifier.padding(horizontal = PaddingNormal)
                                        .animateItem()
                                )
                                if (index < it.lastIndex) {
                                    Spacer(Modifier.height(PaddingNormal))
                                    Spacer(
                                        Modifier.fillMaxWidth().padding(start = PaddingNormal)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.surfaceBright)
                                    )
                                }
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
                        QuizSkeleton(modifier = Modifier.padding(horizontal = PaddingNormal))
                        if (index < 7) {
                            Spacer(Modifier.height(PaddingNormal))
                            Spacer(
                                Modifier.fillMaxWidth().padding(start = PaddingNormal)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceBright)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Serializable
data class SessionStarterDataModel(
    val quizIds: List<Long> = emptyList(),
    val dimensionIds: List<Long> = emptyList(),
)
