package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.concatOrThrow
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.database.TextFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.image_span

sealed interface Frame {
    suspend fun getPreviewText(): String

    data class Text(val textFrame: TextFrame) : Frame {
        override suspend fun getPreviewText(): String {
            return textFrame.content
        }
    }

    data class Image(val imageFrame: ImageFrame) : Frame {
        override suspend fun getPreviewText(): String {
            return imageFrame.altText ?: getString(Res.string.image_span)
        }
    }

    data class Options(val optionsFrame: OptionsFrame, val frames: List<Frame>) : Frame {
        override suspend fun getPreviewText(): String {
            return optionsFrame.name
                ?: frames.mapIndexed { index, frame -> "$index. ${frame.getPreviewText()}" }
                    .joinToString("; ")
        }
    }
}

data class FramedQuiz(val quiz: Quiz, val frames: List<Frame>)

private fun getL1FrameFlow(
    textFrameQuery: Query<TextFrame>,
    imageFrameQuery: Query<ImageFrame>,
): Flow<List<Frame>> {
    val textFrameFlow: Flow<List<Frame>> =
        textFrameQuery
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { textFrame ->
                textFrame.map(Frame::Text)
            }
    val imageFrameFlow: Flow<List<Frame>> =
        imageFrameQuery
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map {
                it.map(Frame::Image)
            }
    return textFrameFlow.concatOrThrow(imageFrameFlow)
}

fun QuizQueries.getFramedQuizzes(starter: Query<Quiz>): Flow<List<FramedQuiz>> =
    starter.asFlow()
        .mapToList(Dispatchers.IO)
        .map { quizzes ->
            quizzes.map { quiz ->
                val optionsFrameFlow: Flow<List<Frame>> = getOptionsFrameByQuizId(quiz.id)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { frames ->
                        coroutineScope {
                            frames.map { optionsFrame ->
                                async {
                                    Frame.Options(
                                        optionsFrame = optionsFrame,
                                        frames = getL1FrameFlow(
                                            textFrameQuery = getTextFrameByOptionsFrameId(
                                                optionsFrame.id
                                            ),
                                            imageFrameQuery = getImageFramesByOptionsFrameId(
                                                optionsFrame.id
                                            )
                                        ).last()
                                    )
                                }
                            }.awaitAll()
                        }
                    }

                FramedQuiz(
                    quiz = quiz,
                    frames = optionsFrameFlow.last()
                )
            }
        }