package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.FloatingPopupButton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.database.Template
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.DimensionViewModel
import com.zhufucdev.practiso.viewmodel.QuizzesViewModel
import com.zhufucdev.practiso.viewmodel.TemplateViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.add_item_to_get_started_para
import practiso.composeapp.generated.resources.baseline_import
import practiso.composeapp.generated.resources.create_para
import practiso.composeapp.generated.resources.dimensions_para
import practiso.composeapp.generated.resources.import_para
import practiso.composeapp.generated.resources.library_is_empty_para
import practiso.composeapp.generated.resources.questions_para
import practiso.composeapp.generated.resources.templates_para

@Composable
fun LibraryApp(
    templateViewModel: TemplateViewModel = viewModel(factory = TemplateViewModel.Factory),
    dimensionViewModel: DimensionViewModel = viewModel(factory = DimensionViewModel.Factory),
    quizzesViewModel: QuizzesViewModel = viewModel(factory = QuizzesViewModel.Factory),
) {
    var showActions by remember {
        mutableStateOf(false)
    }
    val coroutine = rememberCoroutineScope()

    composeFromBottomUp("fab") {
        FloatingPopupButton(
            expanded = showActions,
            onExpandedChange = { showActions = it },
            autoCollapse = true
        ) {
            item(
                label = { Text(stringResource(Res.string.import_para)) },
                icon = {
                    Icon(
                        painterResource(Res.drawable.baseline_import),
                        contentDescription = null
                    )
                },
                onClick = { }
            )
            item(
                label = { Text(stringResource(Res.string.create_para)) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    coroutine.launch {
                        Navigator.navigate(Navigation.Goto(AppDestination.QuizCreate))
                    }
                }
            )
        }
    }

    val templates by templateViewModel.templates.collectAsState(null)
    val dimensions by dimensionViewModel.dimensions.collectAsState(null)
    val quizzes by quizzesViewModel.quiz.collectAsState(null)

    AnimatedContent(templates?.isEmpty() == true && dimensions?.isEmpty() == true && quizzes?.isEmpty() == true) { empty ->
        if (empty) {
            AlertHelper(
                header = { Text("📝") },
                label = { Text(stringResource(Res.string.library_is_empty_para)) },
                helper = { Text(stringResource(Res.string.add_item_to_get_started_para)) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(start = PaddingNormal, top = PaddingNormal)
                    .fillMaxWidth(),
            ) {
                flatContent(
                    value = templates,
                    caption = {
                        SectionCaption(stringResource(Res.string.templates_para))
                    },
                    content = {
                        ContentSkeleton(
                            label = { Text(it.name) },
                            preview = {
                                it.description?.let { p ->
                                    Text(p, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        )
                    },
                    id = Template::id
                )

                flatContent(
                    value = dimensions,
                    caption = {
                        SectionCaption(stringResource(Res.string.dimensions_para))
                    },
                    content = {
                        ContentSkeleton(
                            label = { Text(it.titleString()) },
                            preview = {
                                Text(
                                    text = it.previewString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    },
                    id = { it.dimension.id }
                )

                flatContent(
                    value = quizzes,
                    caption = {
                        SectionCaption(stringResource(Res.string.questions_para))
                    },
                    content = {
                        ContentSkeleton(
                            label = { Text(it.titleString()) },
                            preview = {
                                Text(
                                    text = it.previewString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable {
                                coroutine.launch {
                                    Navigator.navigate(
                                        Navigation.Goto(AppDestination.QuizCreate),
                                        options = listOf(
                                            NavigationOption.OpenQuiz(it.quiz.id)
                                        )
                                    )
                                }
                            }
                        )
                    },
                    id = { it.quiz.id }
                )
            }
        }
    }
}


fun <T> LazyListScope.flatContent(
    value: List<T>?,
    caption: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
    id: (T) -> Any,
    skeleton: @Composable () -> Unit = { ContentSkeleton() },
    skeletonsCount: Int = 3,
) {
    if (value?.isEmpty() == true) {
        return
    }

    item {
        caption()
    }

    value?.let { t ->
        t.forEachIndexed { index, v ->
            item(id(v)) {
                content(v)
                if (index < t.lastIndex) {
                    HorizontalSeparator()
                }
            }
        }
    } ?: items(skeletonsCount) { i ->
        skeleton()
        if (i < skeletonsCount - 1) {
            HorizontalSeparator()
        }
    }
}

@Composable
private fun ContentSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.height(LocalTextStyle.current.lineHeight.value.dp)
                .fillMaxWidth(fraction = 0.618f)
                .shimmerBackground()
        )
    },
    preview: (@Composable () -> Unit)? = {
        Spacer(
            Modifier.height(LocalTextStyle.current.lineHeight.value.dp)
                .fillMaxWidth()
                .shimmerBackground()
        )
    },
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingSmall),
        modifier = modifier
    ) {
        Spacer(Modifier.height(PaddingNormal - PaddingSmall))

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleMedium
        ) {
            label()
        }

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            preview?.invoke()
        }

        Spacer(Modifier.height(PaddingNormal - PaddingSmall))
    }
}

@Composable
private fun HorizontalSeparator() {
    Spacer(
        Modifier.height(1.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    )
}