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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.DialogWithTextInput
import com.zhufucdev.practiso.composable.FabCreate
import com.zhufucdev.practiso.composable.FlipCard
import com.zhufucdev.practiso.composable.HorizontalControl
import com.zhufucdev.practiso.composable.HorizontalDraggable
import com.zhufucdev.practiso.composable.HorizontalDraggingControlTargetWidth
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.PlaceHolder
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.SharedElementTransitionPopupScope
import com.zhufucdev.practiso.composable.SharedHorizontalDraggableExclusion
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.TopLevelDestination
import com.zhufucdev.practiso.composition.combineClickable
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.composition.withExclusionLock
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.calculateTakeCorrectQuizCount
import com.zhufucdev.practiso.datamodel.calculateTakeNumber
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createOptionView
import com.zhufucdev.practiso.platform.createSessionCreatorView
import com.zhufucdev.practiso.service.FeiDbState
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.style.PaddingSpace
import com.zhufucdev.practiso.viewmodel.SessionViewModel
import com.zhufucdev.practiso.viewmodel.SharedElementTransitionPopupViewModel
import com.zhufucdev.practiso.viewmodel.TakeStarterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.accuracy_slash_completeness_para
import resources.baseline_arrow_collapse_up
import resources.baseline_check_circle_outline
import resources.baseline_chevron_down
import resources.baseline_eye_off_outline
import resources.baseline_flag_checkered
import resources.baseline_pin
import resources.baseline_pin_outline
import resources.baseline_timelapse
import resources.baseline_timer_outline
import resources.cancel_para
import resources.continue_or_start_new_take_para
import resources.created_x_para
import resources.done_questions_completed_in_total
import resources.edit_para
import resources.get_started_by_para
import resources.interact_with_status_bar_to_continue_span
import resources.loading_recommendations_span
import resources.loading_takes_para
import resources.minutes_span
import resources.n_percentage
import resources.n_questions_correct_span
import resources.n_questions_incorrect_span
import resources.new_take_para
import resources.new_timer_para
import resources.no_recommendations_span
import resources.no_take_available_span
import resources.pin_para
import resources.quickly_start_new_session_para
import resources.recently_used_para
import resources.remove_para
import resources.rename_para
import resources.rename_session_para
import resources.see_all_options_para
import resources.session_name_para
import resources.session_para
import resources.sessions_para
import resources.show_hidden_para
import resources.spent_x_para
import resources.start_para
import resources.take_completeness
import resources.take_n_para
import resources.use_smart_recommendations_para
import resources.waiting_for_inference_para
import resources.welcome_to_app_para
import resources.will_be_identified_as_x_para
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun SessionApp(model: SessionViewModel = viewModel(factory = SessionViewModel.Factory)) {
    val takeStats by model.recentTakeStats.collectAsState(Dispatchers.IO)
    val sessions by model.sessions.collectAsState(Dispatchers.IO)
    val coroutine = rememberCoroutineScope()
    var renamingSession by remember { mutableStateOf<Session?>(null) }

    SharedElementTransitionPopup(
        key = "quickstart",
        popup = {
            Card(
                shape = FloatingActionButtonDefaults.extendedFabShape,
                modifier = Modifier
                    .pseudoClickable()
            ) {
                Column(Modifier.padding(PaddingBig).fillMaxWidth().height(450.dp)) {
                    SimplifiedSessionCreationModalContent(
                        model = model,
                        columnScope = this,
                        popupScope = this@SharedElementTransitionPopup
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
            PlaceHolder(
                header = {
                    Text("👋")
                },
                label = {
                    Text(stringResource(Res.string.welcome_to_app_para))
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
                        takeStats?.let { stats ->
                            items(stats, TakeStat::id) { takeStat ->
                                val key = "take_" + takeStat.id
                                SharedElementTransitionPopup(
                                    model = viewModel(
                                        key = key,
                                        factory = SharedElementTransitionPopupViewModel.Factory
                                    ),
                                    key = key,
                                    popup = {
                                        val takeNumber by calculateTakeNumber(
                                            model.db, takeStat.id
                                        ).collectAsState(null, Dispatchers.IO)
                                        val quizzesDoneCorrect by calculateTakeCorrectQuizCount(
                                            model.db, takeStat.id
                                        ).collectAsState(null, Dispatchers.IO)

                                        Card(
                                            modifier = Modifier.clickable(false) {},
                                            shape = FloatingActionButtonDefaults.extendedFabShape
                                        ) {
                                            TakeStatExtensionCardContent(
                                                takeStat,
                                                takeNumber,
                                                quizzesDoneCorrect,
                                                onStart = {
                                                    coroutine.launch {
                                                        model.event.startTake.send(takeStat.id)
                                                    }
                                                },
                                                onClickPin = {
                                                    coroutine.launch {
                                                        model.event.toggleTakePin.send(takeStat.id)
                                                    }
                                                },
                                                onDismiss = {
                                                    coroutine.launch {
                                                        collapse()
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    sharedElement = {
                                        Card(modifier = it) { TakeStatCardContent(takeStat) }
                                    }
                                ) {
                                    Card(
                                        modifier = Modifier.animateItem()
                                            .sharedElement()
                                            .clip(CardDefaults.shape)
                                            .combineClickable(
                                                onClick = {
                                                    coroutine.launch {
                                                        Navigator.navigate(
                                                            Navigation.Goto(AppDestination.Answer),
                                                            NavigationOption.OpenTake(
                                                                takeStat.id
                                                            )
                                                        )
                                                    }
                                                },
                                                onSecondaryClick = {
                                                    coroutine.launch {
                                                        expand()
                                                    }
                                                }
                                            )
                                    ) { TakeStatCardContent(takeStat) }
                                }
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
                    items(sessions.size, key = { "session_${sessions[it].id}" }) { index ->
                        val option = sessions[index]
                        ListItem(
                            separator = index < sessions.lastIndex,
                            onEdit = {
                                renamingSession = option.session
                            },
                            onDelete = {
                                coroutine.launch {
                                    model.event.deleteSession.send(option.session.id)
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            val key = "session_" + option.session.id
                            SharedElementTransitionPopup(
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
                                        PractisoOptionView(
                                            option,
                                            modifier = it.padding(PaddingNormal)
                                        )
                                    }
                                }
                            ) {
                                Box(
                                    Modifier.sharedElement()
                                        .clickable { coroutine.launch { expand() } }
                                ) {
                                    PractisoOptionView(
                                        option,
                                        modifier = Modifier.padding(PaddingNormal)
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
                        PractisoOptionSkeleton(modifier = Modifier.padding(PaddingNormal))
                    }
                }

                item("space") {
                    Spacer(Modifier.height(PaddingSpace))
                }
            }
        }
    }

    renamingSession?.let { session ->
        withExclusionLock(SharedHorizontalDraggableExclusion) {
            LaunchedEffect(true) {
                lock()
            }

            SessionRenameDialog(
                currentName = session.name,
                onDismissRequest = { renamingSession = null },
                onRename = {
                    coroutine.launch {
                        model.event.renameSession.send(session.id to it)
                    }
                }
            )
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
private fun TakeStatCardContent(model: TakeStat) {
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
private fun TakeSkeleton(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TakeStatExtensionCardContent(
    model: TakeStat,
    takeNumber: Int?,
    correctQuizCount: Int?,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    onClickPin: () -> Unit,
) {
    Box {
        IconButton(
            content = {
                val pinned = model.pinned == 1L
                Icon(
                    painter = painterResource(if (pinned) Res.drawable.baseline_pin else Res.drawable.baseline_pin_outline),
                    contentDescription = stringResource(Res.string.pin_para),
                    tint = if (pinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            },
            onClick = onClickPin,
            modifier = Modifier.align(Alignment.TopEnd).padding(PaddingNormal)
        )

        Column(
            Modifier.padding(PaddingBig).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PaddingNormal)
        ) {
            Row {
                Text(
                    model.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(36.dp))
            }
            takeNumber?.let {
                Text(stringResource(Res.string.take_n_para, it))
            } ?: Spacer(Modifier.height(LocalTextStyle.current.lineHeight.value.dp).width(40.dp))
            Text(
                stringResource(
                    Res.string.created_x_para,
                    HumanReadable.timeAgo(
                        model.creationTimeISO,
                        Clock.System.now()
                    )
                )
            )
            Text(
                stringResource(
                    Res.string.spent_x_para,
                    HumanReadable.duration(model.durationSeconds.seconds)
                )
            )

            Column {
                val coroutine = rememberCoroutineScope()
                Text(stringResource(Res.string.accuracy_slash_completeness_para))
                Spacer(Modifier.height(PaddingSmall))

                var targetScale by remember { mutableStateOf(1f) }
                Surface(
                    shape = CardDefaults.shape,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        targetScale = 1f
                    }) {
                    val scale by animateFloatAsState(targetScale)

                    Row(Modifier.fillMaxWidth().height(26.dp)) {
                        val properScale by remember(correctQuizCount, model) {
                            derivedStateOf {
                                if (model.countQuizDone > 0 && model.countQuizTotal > model.countQuizDone * 10) {
                                    model.countQuizTotal * 0.618f / model.countQuizDone
                                } else {
                                    1f
                                }
                            }
                        }
                        val correctRatio by remember(correctQuizCount, model) {
                            derivedStateOf {
                                (correctQuizCount ?: 0) * 1f / model.countQuizTotal * scale
                            }
                        }
                        val incorrectRatio by remember(correctQuizCount, model) {
                            derivedStateOf {
                                (correctQuizCount?.let {
                                    (model.countQuizDone - it)
                                } ?: 0) * 1f / model.countQuizTotal * scale
                            }
                        }

                        val correctTooltipState = rememberTooltipState()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                correctQuizCount?.let {
                                    PlainTooltip {
                                        Text(
                                            pluralStringResource(
                                                Res.plurals.n_questions_correct_span, it, it
                                            )
                                        )
                                    }
                                }
                            },
                            state = correctTooltipState
                        ) {
                            Spacer(
                                Modifier.fillMaxWidth(correctRatio).fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        targetScale = if (scale <= 1) {
                                            properScale
                                        } else {
                                            1f
                                        }
                                        coroutine.launch {
                                            correctTooltipState.show()
                                        }
                                    }
                            )
                        }
                        val incorrectTooltipState = rememberTooltipState()
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                correctQuizCount?.let { (model.countQuizDone - it).toInt() }?.let {
                                    PlainTooltip {
                                        Text(
                                            pluralStringResource(
                                                Res.plurals.n_questions_incorrect_span, it, it
                                            )
                                        )
                                    }
                                }
                            },
                            state = incorrectTooltipState
                        ) {
                            Spacer(
                                Modifier.fillMaxWidth(incorrectRatio / (1 - correctRatio))
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable {
                                        targetScale = if (scale <= 1) {
                                            properScale
                                        } else {
                                            1f
                                        }
                                        coroutine.launch {
                                            correctTooltipState.show()
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel_para))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onStart) {
                    Text(stringResource(Res.string.start_para))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SimplifiedSessionCreationModalContent(
    model: SessionViewModel,
    columnScope: ColumnScope,
    popupScope: SharedElementTransitionPopupScope,
) {
    val useRecommendations by model.useRecommendations.collectAsState()
    val smartRec by model.smartRecommendations.collectAsState(null)
    val recentRec by model.recentRecommendations.collectAsState(null)
    val items by remember(useRecommendations) {
        derivedStateOf {
            if (useRecommendations) smartRec
            else recentRec
        }
    }

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

    if (items == null) {
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
            checked = useRecommendations,
            onCheckedChange = { coroutine.launch { model.event.toggleRecommendations.send(it) } }
        )
    }

    Spacer(Modifier.height(PaddingNormal))

    val currentIndex by model.currentCreatorIndex.collectAsState()
    items.let {
        if (it?.isNotEmpty() == true) {
            val coroutine = rememberCoroutineScope()
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                    .fillMaxWidth()
            ) {
                it.forEachIndexed { index, option ->
                    Surface(
                        shape = CardDefaults.shape,
                        color =
                            if (index == currentIndex) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent,
                        onClick = {
                            coroutine.launch {
                                model.event.toggleCreator.send(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(PaddingSmall)) {
                            val view = createOptionView(option)
                            Text(
                                view.title(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                view.preview(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        it?.isEmpty() == true -> Text(
                            stringResource(Res.string.no_recommendations_span),
                            modifier = Modifier.align(Alignment.Center)
                        )

                        it == null -> {
                            val feiState by Database.fei.getUpgradeState().collectAsState(null)
                            when (feiState) {
                                is FeiDbState.Ready ->
                                    Text(
                                        stringResource(Res.string.loading_recommendations_span),
                                        modifier = Modifier.align(Alignment.Center),
                                        textAlign = TextAlign.Center
                                    )

                                else -> Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        stringResource(Res.string.waiting_for_inference_para),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        stringResource(Res.string.interact_with_status_bar_to_continue_span),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
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
        val currentName =
            items?.takeIf { currentIndex >= 0 }
                ?.let { createSessionCreatorView(it[currentIndex]) }
                ?.preview()
        FilledTonalButton(
            enabled = currentIndex >= 0 && currentName != null,
            onClick = {
                items?.let { items ->
                    coroutine.launch {
                        model.event.createSessionStartImmediately
                            .send(currentName!! to items[currentIndex].selection)
                    }
                }
            },
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
            Box(Modifier.padding(start = PaddingNormal)) {
                HorizontalSeparator()
            }
        }
    }
}

@Composable
private fun ColumnScope.TakeStarterContent(
    model: TakeStarterViewModel,
) {
    val takes by model.takeStats.collectAsState(null, Dispatchers.IO)
    val visibleTakes by remember(takes) {
        derivedStateOf { takes?.filter { it.hidden == 0L } }
    }
    val hiddenTakes by remember(takes) {
        derivedStateOf { takes?.filter { it.hidden == 1L } }
    }
    val coroutine = rememberCoroutineScope()
    val option by model.option.collectAsState()
    val view = option?.let(::createOptionView)
    Text(
        view?.title() ?: stringResource(Res.string.loading_takes_para),
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
    val takes by model.takeStats.collectAsState(null, Dispatchers.IO)
    val number by remember(takes) { derivedStateOf { takes?.let { it.size + 1 } } }
    val coroutine = rememberCoroutineScope()

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
    LazyColumn(
        Modifier.weight(1f).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            if (model.currentTimer.id.isNotEmpty()) {
                coroutine.launch {
                    model.event.updateTimerAndClose.send(Unit)
                }
            }
        }) {
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
                            val text =
                                ((timer.duration.toDouble(DurationUnit.MINUTES) * 100).roundToInt() / 100f).toString()
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
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (initialized && !it.hasFocus) {
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

@Composable
private fun SessionRenameDialog(
    currentName: String,
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
) {
    var nameBuffer by remember { mutableStateOf(currentName) }
    val coroutine = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        DialogWithTextInput(
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            title = { Text(stringResource(Res.string.rename_session_para)) },
            inputValue = nameBuffer,
            onInputValueChange = { nameBuffer = it },
            label = { Text(stringResource(Res.string.session_name_para)) },
            singleLine = true,
            positiveButton = {
                Button(
                    enabled = nameBuffer.isNotBlank(),
                    onClick = {
                        coroutine.launch {
                            onRename(nameBuffer)
                            onDismissRequest()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.rename_para))
                }
            },
            negativeButton = {
                OutlinedButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(Res.string.cancel_para))
                }
            }
        )
    }
}
