package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.TopLevelDestination
import com.zhufucdev.practiso.composable.AlertHelper
import com.zhufucdev.practiso.composable.FabCreate
import com.zhufucdev.practiso.composable.FlipCard
import com.zhufucdev.practiso.composable.HorizontalControl
import com.zhufucdev.practiso.composable.HorizontalDraggable
import com.zhufucdev.practiso.composable.HorizontalDraggingControlTargetWidth
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
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.SessionViewModel
import com.zhufucdev.practiso.viewmodel.SharedElementTransitionPopupViewModel
import com.zhufucdev.practiso.viewmodel.TakeStarterViewModel
import kotlinx.coroutines.launch
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_arrow_collapse_up
import practiso.composeapp.generated.resources.baseline_check_circle_outline
import practiso.composeapp.generated.resources.baseline_chevron_down
import practiso.composeapp.generated.resources.baseline_eye_off_outline
import practiso.composeapp.generated.resources.baseline_flag_checkered
import practiso.composeapp.generated.resources.baseline_timelapse
import practiso.composeapp.generated.resources.baseline_timer_outline
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.continue_or_start_new_take_para
import practiso.composeapp.generated.resources.done_questions_completed_in_total
import practiso.composeapp.generated.resources.edit_para
import practiso.composeapp.generated.resources.get_started_by_para
import practiso.composeapp.generated.resources.loading_recommendations_span
import practiso.composeapp.generated.resources.loading_takes_para
import practiso.composeapp.generated.resources.minutes_span
import practiso.composeapp.generated.resources.n_percentage
import practiso.composeapp.generated.resources.new_take_para
import practiso.composeapp.generated.resources.new_timer_para
import practiso.composeapp.generated.resources.no_recommendations_span
import practiso.composeapp.generated.resources.no_take_available_span
import practiso.composeapp.generated.resources.quickly_start_new_session_para
import practiso.composeapp.generated.resources.recently_used_para
import practiso.composeapp.generated.resources.remove_para
import practiso.composeapp.generated.resources.see_all_options_para
import practiso.composeapp.generated.resources.session_para
import practiso.composeapp.generated.resources.sessions_para
import practiso.composeapp.generated.resources.show_hidden_para
import practiso.composeapp.generated.resources.start_para
import practiso.composeapp.generated.resources.take_completeness
import practiso.composeapp.generated.resources.take_n_para
import practiso.composeapp.generated.resources.use_smart_recommendations_para
import practiso.composeapp.generated.resources.welcome_to_app_para
import practiso.composeapp.generated.resources.will_be_identified_as_x_para
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

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
                Column(Modifier.padding(PaddingBig).fillMaxWidth().height(450.dp)) {
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
            FabCreate(modifier = it, onClick = {}, noShadow = true)
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
                        userScrollEnabled = takeStats != null,
                        modifier = Modifier.animateItem()
                    ) {
                        item("start_spacer") {
                            Spacer(Modifier)
                        }
                        takeStats?.let {
                            items(it, TakeStat::id) { takeStat ->
                                Card(
                                    onClick = {
                                        coroutine.launch {
                                            Navigator.navigate(
                                                Navigation.Goto(AppDestination.Answer),
                                                options = listOf(NavigationOption.OpenTake(takeStat.id))
                                            )
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                ) { TakeStatCardContent(takeStat) }
                            }
                        } ?: items(3) {
                            Card { TakeSkeleton() }
                        }
                        item("end_spacer") {
                            Spacer(Modifier)
                        }
                    }
                    Spacer(Modifier.height(PaddingNormal))
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
                                modifier = Modifier.padding(start = PaddingNormal).animateItem()
                            ) {
                                val key = "session_" + option.session.id
                                SharedElementTransitionPopup(
                                    model = viewModel(
                                        key = key,
                                        factory = SharedElementTransitionPopupViewModel.Factory
                                    ),
                                    key = key,
                                    popup = {
                                        val tsModel: TakeStarterViewModel = viewModel(
                                            key = key,
                                            factory = TakeStarterViewModel.Factory
                                        )

                                        LaunchedEffect(option) {
                                            tsModel.load(option, coroutine)
                                        }

                                        FlipCard(
                                            modifier = Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {}
                                            ),
                                            state = tsModel.flipCardState
                                        ) { page ->
                                            Column(Modifier.padding(PaddingBig).height(450.dp)) {
                                                when (page) {
                                                    0 -> TakeStarterContent(model = tsModel)
                                                    1 -> NewTakeContent(
                                                        model = tsModel
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    sharedElement = {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                                            PractisoOptionView(option, modifier = it)
                                        }
                                    }
                                ) {
                                    PractisoOptionView(
                                        option,
                                        modifier = Modifier.sharedElement()
                                            .clickable { coroutine.launch { expand() } }
                                    )
                                }
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

                item("space") {
                    Spacer(Modifier.height(PaddingSpace))
                }
            }
        }
    }
}

@Composable
private fun TakeStatIcon(model: TakeStat) {
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
}

@Composable
fun TakeStatCardContent(model: TakeStat) {
    TakeSkeleton(
        icon = {
            TakeStatIcon(model)
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
private fun ColumnScope.SimplifiedSessionCreationModalContent(
    model: SessionViewModel,
    columnScope: ColumnScope,
    popupScope: SharedElementTransitionPopupScope,
    onCreate: () -> Unit,
) {
    var loadingRecommendations by remember { mutableStateOf(true) }

    Text(
        stringResource(Res.string.session_para),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = with(columnScope) { Modifier.align(Alignment.CenterHorizontally) }
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        stringResource(Res.string.quickly_start_new_session_para),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
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
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                    .fillMaxWidth()
            ) {
                it.forEach {
                    Surface(
                        shape = CardDefaults.shape,
                        color = Color.Transparent,
                        onClick = {

                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(PaddingSmall)) {
                            Text(
                                it.titleString(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                it.previewString(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
    Column(modifier) {
        HorizontalDraggable(
            enabled = swipable,
            targetWidth = HorizontalDraggingControlTargetWidth * 2 + PaddingSmall * 3,
            controls = {
                HorizontalControl(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.clickable(
                        enabled = onEdit != null,
                        onClick = { onEdit?.invoke() })
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.edit_para)
                    )
                }
                HorizontalControl(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.clickable(
                        enabled = onDelete != null,
                        onClick = { onDelete?.invoke() })
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.remove_para)
                    )
                }
            },
            content = content
        )

        if (separator) {
            HorizontalSeparator()
        }
    }
}

@Composable
private fun ColumnScope.TakeStarterContent(
    model: TakeStarterViewModel,
) {
    val takes by model.takeStats.collectAsState()
    val visibleTakes by remember(takes) {
        derivedStateOf { takes?.filter { it.hidden == 0L } }
    }
    val hiddenTakes by remember(takes) {
        derivedStateOf { takes?.filter { it.hidden == 1L } }
    }
    val coroutine = rememberCoroutineScope()
    val option by model.option.collectAsState()
    Text(
        option?.titleString() ?: stringResource(Res.string.loading_takes_para),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        stringResource(Res.string.continue_or_start_new_take_para),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    if (takes?.isNotEmpty() == true) {
        Spacer(Modifier.height(PaddingNormal))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            visibleTakes!!.forEachIndexed { index, stat ->
                item(stat.id) {
                    val state = rememberSwipeToDismissBoxState()
                    LaunchedEffect(state.currentValue) {
                        if (state.currentValue == SwipeToDismissBoxValue.StartToEnd
                            || state.currentValue == SwipeToDismissBoxValue.EndToStart
                        ) {
                            model.event.hide.send(stat.id)
                        }
                    }
                    SwipeToDismissBox(
                        state = state,
                        backgroundContent = {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .padding(horizontal = PaddingSmall),
                                contentAlignment =
                                    if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                                    else Alignment.CenterEnd
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_eye_off_outline),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    ) {
                        val number by remember(stat) {
                            derivedStateOf {
                                takes!!.indexOf(stat) + 1
                            }
                        }
                        TakeStatItem(
                            modifier = Modifier.fillMaxWidth(),
                            model = stat,
                            color =
                                if (model.currentTakeId == stat.id) MaterialTheme.colorScheme.secondaryContainer
                                else CardDefaults.cardColors().containerColor,
                            number = number,
                            onClick = {
                                coroutine.launch {
                                    model.event.tapTake.send(stat.id)
                                }
                            }
                        )
                    }
                }
            }

            item("show_hidden") {
                val rx by animateFloatAsState(
                    targetValue = if (model.showHidden) 180f else 0f,
                    animationSpec = spring()
                )
                Surface(
                    shape = CardDefaults.shape,
                    color = Color.Transparent,
                    onClick = {
                        coroutine.launch {
                            model.event.toggleShowHidden.send(Unit)
                        }
                    }
                ) {
                    Box(
                        Modifier.padding(PaddingNormal).fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(Res.string.show_hidden_para),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        Icon(
                            painterResource(Res.drawable.baseline_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .graphicsLayer {
                                    rotationX = rx
                                }
                        )
                    }
                }
            }

            hiddenTakes?.takeIf { model.showHidden }?.forEachIndexed { index, stat ->
                item(stat.id) {
                    val state = rememberSwipeToDismissBoxState()
                    LaunchedEffect(state.currentValue) {
                        if (state.currentValue == SwipeToDismissBoxValue.StartToEnd
                            || state.currentValue == SwipeToDismissBoxValue.EndToStart
                        ) {
                            model.event.unhide.send(stat.id)
                        }
                    }
                    SwipeToDismissBox(
                        state = state,
                        backgroundContent = {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .padding(horizontal = PaddingSmall),
                                contentAlignment =
                                    if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                                    else Alignment.CenterEnd
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_arrow_collapse_up),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    ) {
                        val number by remember(stat) {
                            derivedStateOf {
                                takes!!.indexOf(stat) + 1
                            }
                        }
                        TakeStatItem(
                            modifier = Modifier.fillMaxWidth(),
                            model = stat,
                            color =
                                if (model.currentTakeId == stat.id) MaterialTheme.colorScheme.secondaryContainer
                                else CardDefaults.cardColors().containerColor,
                            number = number,
                            onClick = {
                                coroutine.launch {
                                    model.event.tapTake.send(stat.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    } else if (takes != null) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.no_take_available_span),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(PaddingSmall))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(fraction = 0.382f)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape,
        color = Color.Transparent,
        onClick = {
            coroutine.launch { model.event.flip.send(1) }
        }
    ) {
        Row(
            Modifier.padding(PaddingNormal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
        ) {
            Icon(painterResource(Res.drawable.baseline_flag_checkered), contentDescription = null)
            Text(stringResource(Res.string.new_take_para))
        }
    }

    Row(modifier = Modifier.align(Alignment.End)) {
        Button(
            onClick = {
                coroutine.launch { model.event.start.send(model.currentTakeId) }
            },
            enabled = model.currentTakeId >= 0
        ) {
            Text(stringResource(Res.string.start_para))
        }
    }
}

@Composable
private fun TakeStatItem(
    modifier: Modifier = Modifier,
    model: TakeStat,
    color: Color,
    number: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = CardDefaults.shape,
        color = color,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingNormal)
        ) {
            TakeStatIcon(model)
            Text(
                stringResource(Res.string.take_n_para, number),
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(
                    Res.string.n_percentage,
                    (model.countQuizDone * 100f / model.countQuizTotal).roundToInt()
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ColumnScope.NewTakeContent(model: TakeStarterViewModel) {
    val takes by model.takeStats.collectAsState()
    val number by remember(takes) { derivedStateOf { takes?.let { it.size + 1 } } }

    Icon(
        painterResource(Res.drawable.baseline_flag_checkered),
        contentDescription = null,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        stringResource(Res.string.new_take_para),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(PaddingSmall))
    Text(
        number?.let {
            stringResource(
                Res.string.will_be_identified_as_x_para,
                stringResource(Res.string.take_n_para, it)
            )
        } ?: stringResource(Res.string.loading_takes_para),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(PaddingNormal))
    LazyColumn(Modifier.weight(1f)) {
        items(model.timers.size, { model.timers[it].id }) { index ->
            val timer = model.timers[index]
            val state = rememberSwipeToDismissBoxState()
            LaunchedEffect(state.currentValue) {
                if (state.currentValue == SwipeToDismissBoxValue.EndToStart
                    || state.currentValue == SwipeToDismissBoxValue.StartToEnd
                ) {
                    model.timers.removeAt(index)
                }
            }
            val coroutine = rememberCoroutineScope()

            AnimatedContent(model.currentTimer.id == timer.id) { active ->
                if (!active) {
                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = state,
                        backgroundContent = {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .padding(horizontal = PaddingSmall),
                                contentAlignment =
                                    if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                                    else Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.remove_para),
                                )
                            }
                        }
                    ) {
                        TimerSkeleton(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardDefaults.cardColors().containerColor,
                            onClick = {
                                coroutine.launch {
                                    model.event.selectTimer.send(timer.id)
                                }
                            },
                            content = {
                                val duration = HumanReadable.duration(timer.duration)
                                Text(duration)
                            }
                        )
                    }
                } else {
                    val focusRequester = remember { FocusRequester() }
                    var initialized by remember { mutableStateOf(false) }
                    LaunchedEffect(true) {
                        focusRequester.requestFocus()
                        initialized = true
                    }

                    TimerSkeleton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = null
                    ) {
                        var buffer by remember {
                            val text = timer.duration.toDouble(DurationUnit.MINUTES).toString()
                            mutableStateOf(
                                TextFieldValue(
                                    text = text,
                                    selection = TextRange(0, text.length)
                                )
                            )
                        }
                        val fl by remember(buffer.text) {
                            derivedStateOf {
                                buffer.text.toFloatOrNull()
                            }
                        }
                        TextField(
                            value = buffer,
                            onValueChange = {
                                buffer = it
                                fl?.let { f ->
                                    model.currentTimer = timer.copy(duration = f.toDouble().minutes)
                                }
                            },
                            suffix = {
                                Text(stringResource(Res.string.minutes_span))
                            },
                            singleLine = true,
                            isError = fl == null,
                            keyboardActions = KeyboardActions(onDone = {
                                coroutine.launch {
                                    model.event.updateTimerAndClose.send(Unit)
                                }
                            }),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.None
                            ),
                            modifier = Modifier.focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (initialized && !it.isFocused) {
                                        coroutine.launch {
                                            model.event.updateTimerAndClose.send(Unit)
                                        }
                                    }
                                }
                                .onKeyEvent {
                                    if (it.key == Key.Enter) {
                                        coroutine.launch {
                                            model.event.updateTimerAndClose.send(Unit)
                                        }
                                        return@onKeyEvent true
                                    }
                                    false
                                }
                        )
                    }
                }
            }
        }

        item("create_timer") {
            TimerSkeleton(
                modifier = Modifier.fillMaxWidth().animateItem(),
                onClick = {
                    model.timers.add(TakeStarterViewModel.Timer(10.minutes))
                },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null)
                },
                content = {
                    Text(stringResource(Res.string.new_timer_para))
                }
            )
        }
    }

    val coroutine = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                coroutine.launch { model.event.flip.send(0) }
            }
        ) {
            Text(stringResource(Res.string.cancel_para))
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                coroutine.launch {
                    model.event.createAndStart.send(Unit)
                }
            }
        ) {
            Text(stringResource(Res.string.start_para))
        }
    }
}

@Composable
private fun TimerSkeleton(
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    onClick: (() -> Unit)?,
    leadingIcon: @Composable () -> Unit = {
        Icon(
            painterResource(Res.drawable.baseline_timer_outline),
            contentDescription = null
        )
    },
    content: @Composable () -> Unit,
) {
    @Composable
    fun Content() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingSmall)
        ) {
            leadingIcon()
            content()
        }
    }

    if (onClick == null) {
        Surface(
            color = color,
            shape = CardDefaults.shape,
            modifier = modifier
        ) {
            Content()
        }
    } else {
        Surface(
            color = color,
            shape = CardDefaults.shape,
            onClick = onClick,
            modifier = modifier
        ) {
            Content()
        }
    }
}