package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.SharedElementTransitionPopupScope
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.datamodel.QuizIntensity
import com.zhufucdev.practiso.route.DimensionAppRouteParams
import com.zhufucdev.practiso.service.ClusterState
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.DimensionViewModel
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.and_it_is_sad_para
import resources.baseline_creation_outline
import resources.baseline_file_outline
import resources.completed_para
import resources.dimension_is_empty_para
import resources.dimension_not_found_para
import resources.exclude_para
import resources.found_n_items_related_to_x_para
import resources.generate_para
import resources.new_question_para
import resources.preparing_para
import resources.reveal_para
import resources.searching_para
import resources.thinking_para
import resources.you_may_fill_it_automatically_using_fab
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun DimensionApp(
    routeParams: DimensionAppRouteParams,
    model: DimensionViewModel = viewModel(factory = DimensionViewModel.Factory),
) {
    LaunchedEffect(model, routeParams) {
        model.event.init.send(routeParams)
    }

    val quizzes by model.quizzes.collectAsState(null, Dispatchers.IO)
    val dimension: DimensionState by model.dimension.map { dim ->
        if (dim != null) DimensionState.Ok(dim)
        else DimensionState.Missing
    }.collectAsState(DimensionState.Pending, Dispatchers.IO)
    val clustering by model.clusterState.collectAsState(null)

    val coroutine = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    AnimatedContent(quizzes?.isEmpty() to dimension, transitionSpec = {
        fadeIn() togetherWith fadeOut()
    }) { (quizzesEmpty, dimension) ->
        if (dimension !is DimensionState.Missing) {
            if (quizzesEmpty != true) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                    contentPadding = PaddingValues(
                        top = PaddingNormal,
                        start = PaddingNormal,
                        end = PaddingNormal,
                        bottom = PaddingSpace
                    ),
                    userScrollEnabled = quizzes != null
                ) {
                    val quizzes = quizzes
                    if (quizzes != null && clustering == null) {
                        items(quizzes, { it.quiz.id }) { quiz ->
                            var cachedQuiz by remember(quiz) { mutableStateOf(quiz) }

                            SharedElementTransitionPopup(
                                key = quiz.quiz.id.toString(),
                                popup = {
                                    Card(Modifier.pseudoClickable()) {
                                        val navController = currentNavController()
                                        QuizPopupContent(
                                            modifier = Modifier.padding(PaddingBig),
                                            model = cachedQuiz,
                                            dimension = (dimension as DimensionState.Ok).value,
                                            onIntensityChanged = {
                                                cachedQuiz = cachedQuiz.copy(intensity = it)
                                            },
                                            onRemove = {
                                                coroutine.launch {
                                                    collapse()
                                                    model.event.remove.send(listOf(quiz.quiz.id))
                                                }
                                            },
                                            onReveal = {
                                                coroutine.launch {
                                                    collapse()
                                                    navController.navigate(
                                                        LibraryAppViewModel.Revealable(
                                                            id = quiz.quiz.id,
                                                            type = LibraryAppViewModel.RevealableType.Quiz
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                },
                                sharedElement = {
                                    QuizIntensityView(
                                        model = cachedQuiz,
                                        onClick = null,
                                        modifier = it
                                    )
                                },
                                content = {
                                    QuizIntensityView(
                                        modifier = Modifier.sharedElement(),
                                        model = cachedQuiz,
                                        onClick = {
                                            coroutine.launch {
                                                expand()
                                            }
                                        }
                                    )
                                }
                            )

                            LaunchedEffect(cachedQuiz) {
                                delay(1.seconds)
                                model.event.update.send(cachedQuiz.quiz.id to cachedQuiz.intensity)
                            }
                        }
                    } else {
                        items(20) {
                            QuizIntensitySkeleton()
                        }
                    }
                }
            } else {
                PlaceHolder(
                    header = { Text("🫙") },
                    label = { Text(stringResource(Res.string.dimension_is_empty_para)) },
                    helper = { Text(stringResource(Res.string.you_may_fill_it_automatically_using_fab)) },
                )
            }
        } else {
            PlaceHolder(
                header = { Text("💔") },
                label = { Text(stringResource(Res.string.dimension_not_found_para)) },
                helper = { Text(stringResource(Res.string.and_it_is_sad_para)) }
            )
        }
    }

    if (dimension is DimensionState.Ok) {
        composeFromBottomUp("fab") {
            ExtendedFloatingActionButton(
                text = {
                    AnimatedContent(clustering) {
                        when (it) {
                            is ClusterState.Complete -> Text(stringResource(Res.string.completed_para))
                            is ClusterState.Inference -> Text(stringResource(Res.string.thinking_para))
                            ClusterState.Preparing -> Text(stringResource(Res.string.preparing_para))
                            ClusterState.Search -> Text(stringResource(Res.string.searching_para))
                            null -> Text(stringResource(Res.string.generate_para))
                        }
                    }
                },
                icon = {
                    AnimatedContent(clustering) {
                        if (it != null) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                painterResource(Res.drawable.baseline_creation_outline),
                                contentDescription = null
                            )
                        }
                    }
                },
                onClick = {
                    if (clustering == null) {
                        coroutine.launch {
                            model.event.generate.send(Unit)
                        }
                    }
                }
            )
        }
    }

    val snackbars = LocalExtensiveSnackbarState.current
    LaunchedEffect(model) {
        model.clusterState.collect { state ->
            when (state) {
                is ClusterState.Complete -> coroutine.launch {
                    snackbars.showSnackbar(
                        getPluralString(
                            Res.plurals.found_n_items_related_to_x_para,
                            state.found,
                            state.found,
                            (dimension as DimensionState.Ok).value.name
                        ),
                        withDismissAction = true
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun QuizIntensitySkeleton(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {
        Spacer(
            Modifier.size(80.dp, 20.dp)
                .shimmerBackground(RoundedCornerShape(PaddingSmall))
        )
    },
    description: @Composable () -> Unit = {
        Spacer(
            Modifier.size(80.dp, 12.dp)
                .shimmerBackground()
        )
    },
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        OutlinedCard(modifier) {
            QuizIntensitySkeletonContent(title, description)
        }
    } else {
        OutlinedCard(onClick, modifier) {
            QuizIntensitySkeletonContent(title, description)
        }
    }
}

@Composable
private inline fun QuizIntensitySkeletonContent(
    crossinline title: @Composable () -> Unit,
    crossinline description: @Composable () -> Unit,
) {
    Column(
        Modifier.heightIn(min = 120.dp).padding(PaddingNormal).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Icon(painterResource(Res.drawable.baseline_file_outline), contentDescription = null)
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center)
        ) {
            title()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
        ) {
            description()
        }
    }
}

@Composable
fun QuizIntensityView(
    modifier: Modifier = Modifier,
    model: QuizIntensity,
    onClick: (() -> Unit)?,
) {
    QuizIntensitySkeleton(
        modifier = modifier,
        title = {
            Text(
                model.quiz.name?.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_question_para)
            )
        },
        description = { Text("${(model.intensity * 100).roundToInt()}%") },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedElementTransitionPopupScope.QuizPopupContent(
    modifier: Modifier = Modifier,
    dimension: Dimension,
    model: QuizIntensity,
    onIntensityChanged: (Double) -> Unit,
    onRemove: () -> Unit,
    onReveal: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PaddingNormal)) {
        Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
            Text(
                model.quiz.name?.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_question_para),
                style = MaterialTheme.typography.titleLarge
            )
            Text(dimension.name, style = MaterialTheme.typography.labelLarge)
        }

        Column {
            val sliderInteractionSource = remember { MutableInteractionSource() }
            Slider(
                value = model.intensity.toFloat(),
                onValueChange = { onIntensityChanged(it.toDouble()) },
                interactionSource = sliderInteractionSource,
                thumb = {
                    val tooltipState = rememberTooltipState(isPersistent = true)
                    val dragged by sliderInteractionSource.collectIsDraggedAsState()

                    LaunchedEffect(dragged) {
                        if (dragged) {
                            tooltipState.show()
                        } else {
                            tooltipState.dismiss()
                        }
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("${(model.intensity * 100).roundToInt()}%") } },
                        state = tooltipState,
                        enableUserInput = false,
                        focusable = false
                    ) {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row {
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(Res.string.exclude_para))
            }

            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Button(onClick = onReveal) {
                    Text(stringResource(Res.string.reveal_para))
                }
            }
        }
    }
}

private sealed class DimensionState {
    data object Missing : DimensionState()
    data class Ok(val value: Dimension) : DimensionState()
    data object Pending : DimensionState()
}
