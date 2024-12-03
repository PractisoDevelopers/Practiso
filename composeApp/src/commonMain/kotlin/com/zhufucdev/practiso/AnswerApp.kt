package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.zhufucdev.practiso.style.ImageFrameSize
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
    val currentQuiz by remember(quizzes, state) {
        derivedStateOf {
            quizzes?.let { q ->
                state?.let { s ->
                    q[s.currentPage]
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                    }
                },
                navigationIcon = { NavigateUpButton() },
                modifier = Modifier.drawBehind {

                }
            )
        }
    ) { padding ->
        AnimatedContent(state != null) { loaded ->
            if (loaded) {
                HorizontalPager(
                    state = state!!,
                    modifier = Modifier.padding(padding)
                ) {
                    Quiz(
                        modifier = Modifier.padding(horizontal = PaddingNormal).fillMaxSize(),
                        quiz = currentQuiz!!,
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
                    SimpleFrame(frame = frame.frame, imageCache = model.imageCache)
                }

                is Frame.Options -> {
                    val coroutine = rememberCoroutineScope()
                    OptionsFrameSkeleton(
                        label = {
                            frame.frame.optionsFrame.name?.let { Text(it) }
                        },
                        content = {
                            val answerOptionIds by remember {
                                derivedStateOf {
                                    answers?.mapNotNull { it.takeIf { it.quizId == quiz.quiz.id }?.frameId }
                                        ?: emptyList()
                                }
                            }

                            fun answerModel(option: KeyedPrioritizedFrame) =
                                Answer.Option(option.frame.id, frame.frame.id, quiz.quiz.id)

                            frame.frame.frames.forEach { option ->
                                val checked = option.frame.id in answerOptionIds
                                OptionSkeleton(
                                    prefix = {
                                        RadioButton(
                                            selected = checked,
                                            onClick = {
                                                coroutine.launch {
                                                    if (checked) {
                                                        model.event.unanswer.send(answerModel(option))
                                                    } else {
                                                        model.event.answer.send(answerModel(option))
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    content = {
                                        SimpleFrame(
                                            frame = option.frame,
                                            imageCache = model.imageCache
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        coroutine.launch {
                                            if (checked) {
                                                model.event.unanswer.send(answerModel(option))
                                            } else {
                                                model.event.answer.send(answerModel(option))
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
                        modifier = Modifier.size(ImageFrameSize) then modifier
                    )
                }
            )
        }

        is Frame.Text -> {
            TextFrameSkeleton {
                Text(frame.textFrame.content)
            }
        }

        else -> throw NotImplementedError("${frame::class.simpleName} is not simple")
    }
}