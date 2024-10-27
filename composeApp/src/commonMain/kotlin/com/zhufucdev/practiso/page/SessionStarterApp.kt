package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.shimmerBackground
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("🤔", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(PaddingNormal))
                Text(
                    stringResource(Res.string.no_options_available_para),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(Res.string.head_to_library_to_create),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
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

@Composable
private fun DimensionSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.size(100.dp, LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    tailingIcon: @Composable () -> Unit = {},
) {
    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
            modifier = Modifier.padding(PaddingSmall)
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge
            ) {
                label()
            }
            tailingIcon()
        }
    }
}

@Composable
private fun QuizSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.fillMaxWidth().height(LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground(RoundedCornerShape(PaddingSmall))
        )
    },
    preview: @Composable () -> Unit = {
        repeat(2) {
            Spacer(
                Modifier.fillMaxWidth(0.6f).height(LocalTextStyle.current.lineHeight.value.dp)
                    .shimmerBackground(RoundedCornerShape(PaddingSmall))
            )
        }
    },
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall), modifier = modifier) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            label()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            preview()
        }
    }
}

@Serializable
data class SessionStarterDataModel(
    val quizIds: List<Long> = emptyList(),
    val dimensionIds: List<Long> = emptyList(),
)
