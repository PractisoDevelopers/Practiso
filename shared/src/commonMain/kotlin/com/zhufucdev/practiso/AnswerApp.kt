package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.composable.FileImage
import com.zhufucdev.practiso.composable.ImageFrameSkeleton
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.composable.OptionSkeleton
import com.zhufucdev.practiso.composable.OptionsFrameSkeleton
import com.zhufucdev.practiso.composable.TextFrameSkeleton
import com.zhufucdev.practiso.composable.rememberFileImageState
import com.zhufucdev.practiso.composition.rememberClosestTimerAhead
import com.zhufucdev.practiso.composition.toTimerPresentation
import com.zhufucdev.practiso.datamodel.Answer
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import com.zhufucdev.practiso.datamodel.PageStyle
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.SettingsModel
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.platform.wobbleHapticFeedback
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_dots_vertical
import resources.baseline_flip_horizontal
import resources.baseline_flip_vertical
import resources.baseline_view_agenda_outline
import resources.continuous_scrolling_para
import resources.horizontal_pager_para
import resources.loading_quizzes_para
import resources.show_accuracy_para
import resources.take_n_para
import resources.time_is_up_para
import resources.vertical_pager_para
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerApp(model: AnswerViewModel) {
    val state by model.pageState.collectAsState(null)
    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(true) {
        while (true) {
            delay(10.seconds)
            model.event.updateDuration.send(Unit)
        }
    }
    DisposableEffect(true) {
        onDispose {
            runBlocking {
                model.updateDurationDb()
            }
        }
    }

    Scaffold(
        topBar = {
            Box {
                val takeNumber by model.takeNumber.collectAsState()
                val session by model.session.collectAsState()
                val elapsed by model.elapsed.collectAsState()
                val timers by model.timers.collectAsState()
                AppTopBar(
                    takeNumber = takeNumber,
                    secondaryTextState = rememberSecondaryTextState(
                        sessionName = session?.name,
                        elapsed = elapsed,
                        timers = timers.map { it.seconds }
                    ),
                    elapsed = elapsed,
                    scrollBehavior = topBarScrollBehavior
                ) {
                    PagerStyleToggle(model.settings)
                    Menu(model.settings)
                }

                state?.let {
                    LaunchedEffect(it.progress) {
                        val index = maxOf(0, (it.progress * it.quizzes.size).roundToInt() - 1)
                        model.event.updateCurrentQuizIndex.send(index)
                    }
                    val showAccuracy by model.settings.showAccuracy.collectAsState()
                    val answers by model.answers.collectAsState()
                    val errorRanges by remember(answers, it.quizzes, showAccuracy) {
                        derivedStateOf {
                            if (showAccuracy && answers != null)
                                calculateErrorRanges(answers!!, it.quizzes)
                            else emptyList()
                        }
                    }
                    AnswerProgressIndicator(
                        progress = it.progress,
                        errorRanges = errorRanges
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(state) { wrap ->
            when (wrap) {
                is AnswerViewModel.PageState.Column -> {
                    LazyColumn(
                        Modifier.padding(padding).fillMaxSize()
                            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                        state = wrap.state
                    ) {
                        items(items = wrap.quizzes, key = { it.quiz.id }) {
                            Quiz(
                                modifier = Modifier.padding(horizontal = PaddingNormal)
                                    .fillMaxSize(),
                                quiz = it,
                                model = model
                            )
                        }
                    }
                }

                is AnswerViewModel.PageState.Pager.Horizontal -> {
                    HorizontalPager(
                        state = wrap.state,
                        modifier = Modifier.padding(padding),
                        flingBehavior = PagerDefaults.flingBehavior(
                            wrap.state,
                            pagerSnapDistance = PagerSnapDistance.atMost(0),
                            snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium),
                            decayAnimationSpec = rememberSplineBasedDecay(),
                            snapPositionalThreshold = 0.1f
                        )
                    ) { page ->
                        Quiz(
                            modifier = Modifier.padding(horizontal = PaddingNormal)
                                .fillMaxSize()
                                .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                                .verticalScroll(rememberScrollState()),
                            quiz = wrap.quizzes[page],
                            model = model
                        )
                    }
                }

                is AnswerViewModel.PageState.Pager.Vertical -> {
                    VerticalPager(
                        state = wrap.state,
                        modifier = Modifier.padding(padding),
                        flingBehavior = PagerDefaults.flingBehavior(
                            wrap.state,
                            pagerSnapDistance = PagerSnapDistance.atMost(0),
                            snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium),
                            decayAnimationSpec = rememberSplineBasedDecay(),
                            snapPositionalThreshold = 0.1f
                        )
                    ) { page ->
                        Quiz(
                            modifier = Modifier.padding(horizontal = PaddingNormal)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            quiz = wrap.quizzes[page],
                            model = model
                        )
                    }
                }

                null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            PaddingSmall,
                            Alignment.CenterVertically
                        ),
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        CircularProgressIndicator(Modifier.size(64.dp))
                        Text(stringResource(Res.string.loading_quizzes_para))
                    }
                }
            }
        }
    }
}

private sealed interface SecondaryText {
    data class Timer(val incoming: Duration) : SecondaryText
    data object Timeout : SecondaryText
    data class Text(val value: String) : SecondaryText
    data object Hidden : SecondaryText
}

@Composable
private fun rememberSecondaryTextState(
    sessionName: String?,
    elapsed: Duration?,
    timers: List<Duration>,
): SecondaryText {
    val incoming = rememberClosestTimerAhead(elapsed, timers)
    var state by remember {
        mutableStateOf<SecondaryText>(SecondaryText.Hidden)
    }

    LaunchedEffect(sessionName) {
        state = if (sessionName != null) {
            SecondaryText.Text(sessionName)
        } else {
            SecondaryText.Hidden
        }
    }

    LaunchedEffect(incoming) {
        if (state !is SecondaryText.Timeout && state !is SecondaryText.Timer) {
            delay(3.seconds)
        }

        if (incoming != null) {
            state = SecondaryText.Timer(incoming)
        } else if (timers.isNotEmpty()) {
            state = SecondaryText.Timeout
        }
    }

    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    takeNumber: Int?,
    secondaryTextState: SecondaryText,
    elapsed: Duration?,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        title = {
            takeNumber?.let {
                Column(Modifier.animateContentSize()) {
                    Text(stringResource(Res.string.take_n_para, it))

                    AnimatedContent(
                        secondaryTextState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) { state ->
                        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                            when (state) {
                                SecondaryText.Hidden -> {
                                }

                                SecondaryText.Timeout -> {
                                    Text(
                                        stringResource(Res.string.time_is_up_para),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                is SecondaryText.Text -> {
                                    Text(state.value)
                                }

                                is SecondaryText.Timer -> {
                                    elapsed?.let {
                                        Text((state.incoming - it).toTimerPresentation())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = { NavigateUpButton() },
        actions = actions
    )
}

@Composable
private fun Quiz(modifier: Modifier = Modifier, quiz: QuizFrames, model: AnswerViewModel) {
    val answers by model.answers.collectAsState()
    val showAccuracy by model.settings.showAccuracy.collectAsState()
    Column(modifier) {
        quiz.frames.forEach { frame ->
            when (frame.frame) {
                is Frame.Image, is Frame.Text -> {
                    SimpleFrame(
                        frame = frame.frame,
                        imageCache = model.imageCache
                    )
                }

                is Frame.Options -> {
                    val coroutine = rememberCoroutineScope()
                    OptionsFrameSkeleton(
                        label = {
                            frame.frame.optionsFrame.name?.let { Text(it) }
                        },
                        content = {
                            val answerOptionIds by remember(answers) {
                                derivedStateOf {
                                    answers?.mapNotNull { (it.takeIf { it is Answer.Option && it.quizId == quiz.quiz.id } as Answer.Option?)?.optionId }
                                        ?: emptyList()
                                }
                            }
                            val correctChoices by remember(frame) {
                                derivedStateOf {
                                    frame.frame.frames.count { it.isKey }
                                }
                            }

                            fun answerModel(option: KeyedPrioritizedFrame) =
                                Answer.Option(option.frame.id, frame.frame.id, quiz.quiz.id)


                            frame.frame.frames.forEachIndexed { index, option ->
                                val checked = option.frame.id in answerOptionIds
                                val wiggler = remember { Animatable(0f) }
                                val wiggleOffset by wiggler.asState()

                                suspend fun feedbackIncorrectness() {
                                    wobbleHapticFeedback()
                                    wiggler.animateTo(
                                        0f, initialVelocity = 2000f, animationSpec = spring(
                                            Spring.DampingRatioHighBouncy
                                        )
                                    )
                                }

                                suspend fun selectOnly() {
                                    if (checked) {
                                        model.event.unanswer.send(
                                            answerModel(option)
                                        )
                                    } else {
                                        coroutineScope {
                                            launch {
                                                model.event.answer.send(
                                                    answerModel(option)
                                                )
                                                frame.frame.frames.forEachIndexed { i, f ->
                                                    if (i != index) {
                                                        model.event.unanswer.send(
                                                            answerModel(f)
                                                        )
                                                    }
                                                }
                                            }

                                            launch {
                                                if (showAccuracy && !option.isKey) {
                                                    feedbackIncorrectness()
                                                }
                                            }
                                        }
                                    }
                                }

                                suspend fun selectMulti() {
                                    if (checked) {
                                        model.event.unanswer.send(
                                            answerModel(option)
                                        )
                                    } else {
                                        coroutineScope {
                                            launch {
                                                model.event.answer.send(
                                                    answerModel(option)
                                                )
                                            }

                                            launch {
                                                if (showAccuracy && !option.isKey) {
                                                    feedbackIncorrectness()
                                                }
                                            }
                                        }
                                    }
                                }

                                OptionSkeleton(
                                    prefix = {
                                        if (correctChoices <= 1) {
                                            RadioButton(
                                                selected = checked,
                                                enabled = correctChoices > 0,
                                                onClick = {
                                                    coroutine.launch {
                                                        selectOnly()
                                                    }
                                                }
                                            )
                                        } else {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = {
                                                    coroutine.launch {
                                                        selectMulti()
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    content = {
                                        SimpleFrame(
                                            frame = option.frame,
                                            imageCache = model.imageCache,
                                        )
                                    },
                                    modifier = Modifier.clickable(enabled = correctChoices > 0) {
                                        coroutine.launch {
                                            if (correctChoices > 1) {
                                                selectMulti()
                                            } else {
                                                selectOnly()
                                            }
                                        }
                                    } then Modifier.offset(wiggleOffset.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleFrame(modifier: Modifier = Modifier, frame: Frame, imageCache: BitmapRepository) {
    val platform = getPlatform()
    when (frame) {
        is Frame.Image -> {
            ImageFrameSkeleton(
                image = {
                    FileImage(
                        path = frame.imageFrame.filename.takeIf(String::isNotBlank)
                            ?.let { platform.resourcePath.resolve(it) },
                        contentDescription = frame.imageFrame.altText,
                        cache = imageCache,
                        fileSystem = platform.filesystem,
                        state = rememberFileImageState(),
                    )
                },
                altText = {
                    frame.imageFrame.altText?.let {
                        Text(it)
                    }
                },
                modifier = modifier.fillMaxWidth()
            )
        }

        is Frame.Text -> {
            TextFrameSkeleton {
                Text(frame.textFrame.content, modifier)
            }
        }

        else -> throw NotImplementedError("${frame::class.simpleName} is not simple")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PagerStyleToggle(settings: SettingsModel) {
    val current by settings.answerPageStyle.collectAsState()
    val currentName = stringResource(
        when (current) {
            PageStyle.Horizontal -> Res.string.horizontal_pager_para
            PageStyle.Vertical -> Res.string.vertical_pager_para
            PageStyle.Column -> Res.string.continuous_scrolling_para
        }
    )
    val coroutine = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(currentName) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = {
                val next = PageStyle.entries[(current.ordinal + 1) % PageStyle.entries.size]
                coroutine.launch {
                    settings.answerPageStyle.emit(next)
                }
            }
        ) {
            Icon(
                painterResource(
                    when (current) {
                        PageStyle.Horizontal -> Res.drawable.baseline_flip_horizontal
                        PageStyle.Vertical -> Res.drawable.baseline_flip_vertical
                        PageStyle.Column -> Res.drawable.baseline_view_agenda_outline
                    }
                ),
                contentDescription = currentName
            )
        }
    }
}

@Composable
private fun Menu(model: SettingsModel) {
    val coroutine = rememberCoroutineScope()
    Box {
        var expanded by remember { mutableStateOf(false) }
        IconButton(
            onClick = { expanded = true },
        ) {
            Icon(painterResource(Res.drawable.baseline_dots_vertical), contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val showAccuracy by model.showAccuracy.collectAsState()
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.show_accuracy_para)) },
                leadingIcon = { Checkbox(checked = showAccuracy, onCheckedChange = null) },
                onClick = {
                    coroutine.launch {
                        model.showAccuracy.emit(!showAccuracy)
                    }
                }
            )
        }
    }
}

@Composable
private fun AnswerProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    errorRanges: List<OpenEndRange<Float>>,
) {
    val errorColor = MaterialTheme.colorScheme.error
    LinearProgressIndicator(
        progress = { progress },
        drawStopIndicator = {},
        modifier = Modifier.fillMaxWidth()
            .drawWithContent {
                drawContent()
                errorRanges.forEach {
                    drawRect(
                        color = errorColor,
                        topLeft = Offset(x = it.start * size.width, y = 0f),
                        size = Size(
                            width = (it.endExclusive - it.start) * size.width,
                            height = size.height
                        ),
                        blendMode = BlendMode.Hue
                    )
                }
            } then modifier
    )
}

@Suppress("UNCHECKED_CAST")
private fun calculateErrorRanges(
    answers: List<Answer>,
    quizzes: List<QuizFrames>,
): List<OpenEndRange<Float>> = buildList {
    val quizWeight = 1f / quizzes.size
    quizzes.forEachIndexed { quizIndex, option ->
        val quizAnswers = answers.filter { it.quizId == option.quiz.id }
        val answerables =
            option.frames.map(PrioritizedFrame::frame).filterIsInstance<Frame.Answerable<*>>()
        val frameWeight = quizWeight / answerables.size
        answerables.forEachIndexed { frameIndex, frame ->
            val current = quizAnswers.filter { a -> a.frameId == frame.id }
            if (current.isNotEmpty()) {
                when (frame) {
                    is Frame.Options -> {
                        (current as List<Answer.Option>)
                        if (!with(frame) { current.isAdequateNecessary() }) {
                            add((quizWeight * quizIndex + frameWeight * frameIndex).let {
                                it.rangeUntil(it + frameWeight)
                            })
                        }
                    }
                }
            }
        }
    }

    // merge touching ranges
    var i = 0
    while (i < size - 1) {
        val a = get(i)
        val b = get(i + 1)
        if (a.endExclusive == b.start) {
            removeAt(i + 1)
            removeAt(i)
            add(i, a.start.rangeUntil(b.endExclusive))
        }
        i++
    }
}