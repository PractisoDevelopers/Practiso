package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.zhufucdev.practiso.datamodel.Answer
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.loading_quizzes_para
import practiso.composeapp.generated.resources.take_n_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerApp(model: AnswerViewModel) {
    val quizzes by model.quizzes.collectAsState()
    val state by model.pagerState.collectAsState()
    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            Box {
                TopAppBar(
                    title = {
                        val takeNumber by model.takeNumber.collectAsState()
                        val session by model.session.collectAsState()
                        takeNumber?.let {
                            Column(Modifier.animateContentSize()) {
                                Text(stringResource(Res.string.take_n_para, it))

                                session?.let {
                                    Text(
                                        it.name,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    },
                    scrollBehavior = topBarScrollBehavior,
                    navigationIcon = { NavigateUpButton() },
                    modifier = Modifier.drawBehind {

                    }
                )

                state?.let {
                    val weight = 1f / it.pageCount
                    LinearProgressIndicator(
                        progress = {
                            (it.currentPageOffsetFraction + it.currentPage + 1) * weight
                        },
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(state != null) { loaded ->
            if (loaded) {
                HorizontalPager(
                    state = state!!,
                    modifier = Modifier.padding(padding)
                ) { page ->
                    val currentQuiz by remember(quizzes) {
                        derivedStateOf {
                            quizzes!![page]
                        }
                    }
                    Quiz(
                        modifier = Modifier.padding(horizontal = PaddingNormal)
                            .fillMaxSize()
                            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                        quiz = currentQuiz,
                        model = model
                    )
                }
            } else {
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

@Composable
private fun Quiz(modifier: Modifier = Modifier, quiz: QuizFrames, model: AnswerViewModel) {
    val answers by model.answers.collectAsState()
    LazyColumn(modifier) {
        items(quiz.frames, { it.frame::class.simpleName + it.frame.id }) { frame ->
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

                                suspend fun selectOnly() {
                                    if (checked) {
                                        model.event.unanswer.send(
                                            answerModel(option)
                                        )
                                    } else {
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
                                }

                                suspend fun selectMulti() {
                                    if (checked) {
                                        model.event.unanswer.send(
                                            answerModel(option)
                                        )
                                    } else {
                                        model.event.answer.send(
                                            answerModel(option)
                                        )
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
                                            imageCache = model.imageCache
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
                                    }
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