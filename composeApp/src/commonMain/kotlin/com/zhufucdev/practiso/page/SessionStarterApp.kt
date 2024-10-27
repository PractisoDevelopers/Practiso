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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.DimensionSkeleton
import com.zhufucdev.practiso.composable.QuizSkeleton
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.SessionSelectorViewModel
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.head_to_library_to_create
import practiso.composeapp.generated.resources.no_options_available_para

@Composable
fun SessionStarter(
    dataModel: SessionStarterDataModel,
    onDataModelChange: (SessionStarterDataModel) -> Unit,
    appModel: SessionStarterAppViewModel = viewModel(factory = SessionStarterAppViewModel.Factory),
    selectorModel: SessionSelectorViewModel = viewModel(factory = SessionSelectorViewModel.Factory),
) {
    composeFromBottomUp("fab", null)

    val items by appModel.items.collectAsState(null)
    val itemByDimensionId by remember(items) {
        derivedStateOf { items?.associateBy { it.dimension.id } ?: emptyMap() }
    }
    val selectedItems by remember(dataModel, items) {
        derivedStateOf {
            dataModel.takeIf { items != null }?.dimensionIds?.mapNotNull { itemByDimensionId[it] }
                ?.sortedBy { it.dimension.id }
        }
    }
    val quizzes by remember(selectedItems) {
        derivedStateOf { selectedItems?.map { it.quizzes }?.flatten() }
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
                    selectedItems?.let {
                        items(it, key = { d -> d.dimension.id }) { d ->
                            DimensionSkeleton(
                                label = { Text(d.dimension.name) }
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
                    quizzes?.let {
                        it.forEachIndexed { index, quiz ->
                            item(quiz.quiz.id) {
                                QuizSkeleton(
                                    label = { Text(quiz.titleText()) },
                                    preview = { Text(quiz.previewText()) }
                                )
                                if (index < it.lastIndex) {
                                    Spacer(Modifier.height(PaddingNormal))
                                    Spacer(
                                        Modifier.fillMaxWidth().height(1.dp)
                                            .background(MaterialTheme.colorScheme.surfaceBright)
                                    )
                                }
                            }
                        }
                    } ?: items(8) { index ->
                        QuizSkeleton(modifier = Modifier.padding(horizontal = PaddingNormal))
                        if (index < 7) {
                            Spacer(Modifier.height(PaddingNormal))
                            Spacer(
                                Modifier.fillMaxWidth().height(1.dp)
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
