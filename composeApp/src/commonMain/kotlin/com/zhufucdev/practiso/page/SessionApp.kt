package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.TopLevelDestination
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.FabCreate
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.SharedElementTransitionPopupScope
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.SessionViewModel
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
import practiso.composeapp.generated.resources.sessions_para
import practiso.composeapp.generated.resources.start_para
import practiso.composeapp.generated.resources.take_completeness
import practiso.composeapp.generated.resources.use_smart_recommendations_para
import practiso.composeapp.generated.resources.welcome_to_app_para
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SessionApp(
    model: SessionViewModel = viewModel(factory = SessionViewModel.Factory),
) {
    val takeStats by model.recentTakeStats.collectAsState()
    val sessions by model.sessions.collectAsState()
    val coroutine = rememberCoroutineScope()

    SharedElementTransitionPopup(
        key = "quickstart",
        popup = {
            Card(
                shape = FloatingActionButtonDefaults.extendedFabShape,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
            ) {
                Column(Modifier.padding(PaddingBig).fillMaxWidth()) {
                    SimplifiedSessionCreationModalContent(
                        model = model,
                        columnScope = this,
                        popupScope = this@SharedElementTransitionPopup,
                        onCreate = {}
                    )
                }
            }
        },
        sharedElement = {
            FabCreate(modifier = it, onClick = {})
        }
    ) {
        composeFromBottomUp("fab") {
            FabCreate(
                onClick = { coroutine.launch { expand() } },
                modifier = Modifier.sharedElement()
            )
        }
    }

    AnimatedContent(takeStats?.isEmpty() == true && sessions?.isEmpty() == true) { empty ->
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
            LazyColumn(
                Modifier.padding(top = PaddingNormal),
                userScrollEnabled = sessions != null
            ) {
                item("recent_takes_and_captions") {
                    SectionCaption(
                        stringResource(Res.string.recently_used_para),
                        Modifier.padding(start = PaddingNormal)
                    )
                    Spacer(Modifier.height(PaddingSmall))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
                        userScrollEnabled = takeStats != null
                    ) {
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

                item {
                    SectionCaption(
                        stringResource(Res.string.sessions_para),
                        Modifier.padding(start = PaddingNormal)
                    )
                }

                sessions?.let { sessions ->
                    sessions.forEachIndexed { index, option ->
                        item("session_" + option.session.id) {
                            ListItem(
                                separator = index < sessions.lastIndex,
                                onEdit = {},
                                onDelete = {
                                    coroutine.launch {
                                        model.event.deleteSession.send(option.session.id)
                                    }
                                },
                                modifier = Modifier.animateItem()
                            ) {
                                PractisoOptionView(option)
                            }
                        }
                    }
                } ?: items(5) {
                    ListItem(
                        separator = it < 4,
                        swipable = false,
                        modifier = Modifier.animateItem()
                    ) {
                        PractisoOptionSkeleton()
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

@Composable
private fun SimplifiedSessionCreationModalContent(
    model: SessionViewModel,
    columnScope: ColumnScope,
    popupScope: SharedElementTransitionPopupScope,
    onCreate: () -> Unit,
) {
    var loadingRecommendations by remember { mutableStateOf(true) }

    Text(
        stringResource(Res.string.session_para),
        style = MaterialTheme.typography.titleLarge,
        modifier = with(columnScope) { Modifier.align(Alignment.CenterHorizontally) }
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        stringResource(Res.string.quickly_start_new_session_para),
        style = MaterialTheme.typography.labelLarge,
        modifier = with(columnScope) { Modifier.align(Alignment.CenterHorizontally) }
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
        val coroutine = rememberCoroutineScope()
        Switch(
            checked = model.useRecommendations,
            onCheckedChange = { coroutine.launch { model.event.toggleRecommendations.send(it) } }
        )
    }

    Spacer(Modifier.height(PaddingNormal))

    Box(
        modifier = Modifier.height(200.dp).fillMaxWidth()
    ) {
        val items by (
                if (model.useRecommendations) model.smartRecommendations
                else model.recentRecommendations
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
                            Text(it.previewString(), style = MaterialTheme.typography.titleMedium)
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
                popupScope.collapse()
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

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    separator: Boolean,
    swipable: Boolean = true,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragAnimator = remember { Animatable(0f) }
    val coroutine = rememberCoroutineScope()
    val density = LocalDensity.current
    val targetWidth = 160.dp + PaddingSmall * 3

    fun onDropEnd() {
        coroutine.launch {
            dragAnimator.snapTo(dragOffset)
            if (-dragOffset.dp > targetWidth * 0.5f) {
                dragAnimator.animateTo(-targetWidth.value) {
                    dragOffset = value
                }
            } else {
                dragAnimator.animateTo(0f) {
                    dragOffset = value
                }
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.padding(start = PaddingNormal)
                .pointerInput(swipable) {
                    if (!swipable) {
                        return@pointerInput
                    }

                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            if (dragOffset >= 0 && amount > 0) {
                                return@detectHorizontalDragGestures
                            }
                            dragOffset += with(density) { amount.toDp().value }
                            change.consume()
                        },
                        onDragCancel = {
                            onDropEnd()
                        },
                        onDragEnd = {
                            onDropEnd()
                        }
                    )
                }
        ) {
            Surface(Modifier.fillMaxWidth().offset(x = dragOffset.dp)) {
                content()
            }
            if (separator) {
                HorizontalSeparator()
            }
        }

        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Row(
                Modifier.fillMaxHeight().width(-dragOffset.dp)
                    .padding(PaddingSmall),
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                DisguisedButton(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.clickable(
                        enabled = onEdit != null,
                        onClick = { onEdit?.invoke() })
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                DisguisedButton(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.clickable(
                        enabled = onDelete != null,
                        onClick = { onDelete?.invoke() })
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DisguisedButton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    color: Color,
    targetWidth: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val targetPx = with(LocalDensity.current) { targetWidth.roundToPx() }
    Layout(
        modifier = Modifier.weight(1f).fillMaxHeight().background(
            shape = shape,
            color = color
        ).clip(shape).clipToBounds() then modifier,
        content = content
    ) { measurables, constraints ->
        val childConstraints = Constraints(
            maxHeight = (constraints.maxHeight * 0.4).roundToInt()
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                val y = ((layoutHeight - it.height) / 2f).roundToInt()
                if (layoutWidth < targetPx * 2) {
                    it.placeRelative(layoutWidth - targetPx - (it.width / 2f).roundToInt(), y)
                } else {
                    it.placeRelative(((layoutWidth - it.width) / 2f).roundToInt(), y)
                }
            }
        }
    }
}
