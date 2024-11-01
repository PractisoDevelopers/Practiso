package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.TopLevelDestination
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.BackHandlerOrIgnored
import com.zhufucdev.practiso.composable.FabCreate
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.SessionViewModel
import com.zhufucdev.practiso.viewmodel.SimplifiedSessionCreationViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_check_circle_outline
import practiso.composeapp.generated.resources.baseline_timelapse
import practiso.composeapp.generated.resources.done_questions_completed_in_total
import practiso.composeapp.generated.resources.get_started_by_para
import practiso.composeapp.generated.resources.loading_recommendations_span
import practiso.composeapp.generated.resources.no_recommendations_span
import practiso.composeapp.generated.resources.quickly_start_new_session_para
import practiso.composeapp.generated.resources.recently_used_para
import practiso.composeapp.generated.resources.see_all_options_para
import practiso.composeapp.generated.resources.session_para
import practiso.composeapp.generated.resources.start_para
import practiso.composeapp.generated.resources.take_completeness
import practiso.composeapp.generated.resources.use_smart_recommendations_para
import practiso.composeapp.generated.resources.welcome_to_app_para
import kotlin.math.min

const val SessionQuickStarterKey = "session_quickstarter"

@Composable
fun SessionApp(
    sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory),
    sscmViewModel: SimplifiedSessionCreationViewModel =
        viewModel(factory = SimplifiedSessionCreationViewModel.Factory),
) {
    val takeStats by sessionViewModel.recentTakeStats.collectAsState(null)
    val coroutine = rememberCoroutineScope()

    composeFromBottomUp("fab") {
        AnimatedVisibility(
            visible = !sscmViewModel.expanded,
            enter = fadeIn(tween(delayMillis = 230)),
            exit = fadeOut(tween(1))
        ) {
            FabCreate(
                onClick = { coroutine.launch { sscmViewModel.expand() } },
                modifier = Modifier.onGloballyPositioned {
                    sscmViewModel.transitionStart = it.boundsInRoot()
                })
        }
    }

    composeFromBottomUp(SessionQuickStarterKey) {
        if (sscmViewModel.visible) {
            SimplifiedSessionCreationModal(
                model = sscmViewModel,
                sessionModel = sessionViewModel,
                onCreate = {},
            )
        }
    }

    AnimatedContent(takeStats?.isEmpty() == true) { empty ->
        if (empty) {
            AlertHelper(
                label = {
                    Text(stringResource(Res.string.welcome_to_app_para))
                },
                header = {
                    Text("👋")
                },
                helper = {
                    Text(stringResource(Res.string.get_started_by_para))
                }
            )
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState())
                    .padding(top = PaddingNormal)
            ) {
                SectionCaption(
                    stringResource(Res.string.recently_used_para),
                    Modifier.padding(start = PaddingNormal)
                )
                Spacer(Modifier.height(PaddingSmall))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PaddingNormal)) {
                    item("start_spacer") {
                        Spacer(Modifier)
                    }
                    takeStats?.let {
                        items(it, TakeStat::id) { session ->
                            Card { TakeContent(session) }
                        }
                    } ?: items(3) {
                        Card { TakeSkeleton() }
                    }
                    item("end_spacer") {
                        Spacer(Modifier)
                    }
                }
            }
        }

    }

}

@Composable
fun TakeContent(model: TakeStat) {
    TakeSkeleton(
        icon = {
            Icon(
                painter = painterResource(
                    if (model.countQuizDone >= model.countQuizTotal) {
                        Res.drawable.baseline_check_circle_outline
                    } else {
                        Res.drawable.baseline_timelapse
                    }
                ),
                contentDescription = stringResource(Res.string.take_completeness)
            )
        },
        label = {
            Text(
                text = model.name,
                overflow = TextOverflow.Ellipsis
            )
        },
        content = {
            Text(
                pluralStringResource(
                    Res.plurals.done_questions_completed_in_total,
                    min(model.countQuizDone, 10).toInt(),
                    model.countQuizDone,
                    model.countQuizTotal
                )
            )
        },
        progress = model.countQuizDone.toFloat() / model.countQuizTotal
    )
}

@Composable
fun TakeSkeleton(
    icon: @Composable () -> Unit = {
        Box(Modifier.size(32.dp).shimmerBackground(CircleShape))
    },
    label: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth()
                .height(LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    content: @Composable () -> Unit = label,
    progress: Float = 0f,
) {
    val p by animateFloatAsState(targetValue = progress)
    Box {
        Box(Modifier.matchParentSize()) {
            Spacer(
                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    .fillMaxHeight()
                    .fillMaxWidth(p)
            )
        }
        Column(Modifier.padding(PaddingNormal).width(200.dp)) {
            icon()
            Spacer(Modifier.height(PaddingSmall))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall
            ) {
                label()
            }
            Spacer(Modifier.height(PaddingSmall))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium
            ) {
                content()
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SimplifiedSessionCreationModal(
    model: SimplifiedSessionCreationViewModel,
    sessionModel: SessionViewModel,
    onCreate: () -> Unit,
) {
    val coroutine = rememberCoroutineScope()
    SharedTransitionScope { mod ->
        val maskAlpha by animateFloatAsState(
            if (model.expanded) 0.5f else 0f
        )
        Box(
            mod.fillMaxSize().background(Color.Black.copy(alpha = maskAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { coroutine.launch { model.collapse() } }
                )
        ) {
            AnimatedVisibility(model.expanded, modifier = Modifier.align(Alignment.Center)) {
                Card(
                    shape = FloatingActionButtonDefaults.extendedFabShape,
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(PaddingNormal)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("quickstart"),
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}
                        )
                ) {
                    Column(Modifier.padding(PaddingBig).fillMaxWidth()) {
                        SimplifiedSessionCreationModalContent(model, sessionModel, onCreate)
                    }
                }
            }

            AnimatedVisibility(
                !model.expanded,
                modifier = Modifier.offset {
                    IntOffset(
                        model.transitionStart.left.toInt(),
                        model.transitionStart.top.toInt()
                    )
                }
            ) {
                FabCreate(
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState("quickstart"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    ),
                    onClick = {}
                )
            }
        }
    }

    BackHandlerOrIgnored {
        coroutine.launch {
            model.collapse()
        }
    }
}

@Composable
private fun ColumnScope.SimplifiedSessionCreationModalContent(
    model: SimplifiedSessionCreationViewModel,
    sessionModel: SessionViewModel,
    onCreate: () -> Unit,
) {
    var loadingRecommendations by remember { mutableStateOf(true) }

    Text(
        stringResource(Res.string.session_para),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        stringResource(Res.string.quickly_start_new_session_para),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    if (loadingRecommendations) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(PaddingNormal)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(fraction = 0.382f)
            )
        }
    } else {
        Spacer(Modifier.height(PaddingNormal))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
    ) {
        Text(stringResource(Res.string.use_smart_recommendations_para))
        Spacer(Modifier.weight(1f))
        Spacer(
            Modifier.size(height = 26.dp, width = 1.dp)
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Switch(
            checked = model.useRecommendations,
            onCheckedChange = { model.useRecommendations = it }
        )
    }

    Spacer(Modifier.height(PaddingNormal))

    Box(
        modifier = Modifier.height(200.dp).fillMaxWidth()
    ) {
        val items by (
                if (model.useRecommendations) sessionModel.smartRecommendations
                else sessionModel.recentRecommendations
                ).collectAsState(null)

        LaunchedEffect(items) {
            loadingRecommendations = items == null
        }

        items.let {
            if (it?.isNotEmpty() == true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    it.forEach {
                        Surface(
                            shape = CardDefaults.shape,
                            onClick = {

                            },
                            modifier = Modifier.padding(PaddingNormal)
                        ) {
                            Text(it.previewText(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                when {
                    it?.isEmpty() == true -> Text(
                        stringResource(Res.string.no_recommendations_span),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    it == null -> Text(
                        stringResource(Res.string.loading_recommendations_span),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    val navController = currentNavController()
    val coroutine = rememberCoroutineScope()
    Surface(
        onClick = {
            coroutine.launch {
                model.collapse()
                navController.navigate("${TopLevelDestination.Session.route}/new")
            }
        },
        shape = CardDefaults.shape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
            modifier = Modifier.fillMaxWidth().padding(PaddingNormal)
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
            Text(stringResource(Res.string.see_all_options_para))
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        FilledTonalButton(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            content = { Text(stringResource(Res.string.start_para)) }
        )
    }
}