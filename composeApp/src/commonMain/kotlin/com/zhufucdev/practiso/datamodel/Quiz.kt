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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.getString
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.image_span

@Serializable
sealed interface Frame {
    suspend fun getPreviewText(): String

    @Serializable(TextSerializer::class)
    data class Text(val textFrame: TextFrame) : Frame {
        override suspend fun getPreviewText(): String {
            return textFrame.content
        }
    }

    @Serializable(ImageSerializer::class)
    data class Image(val imageFrame: ImageFrame) : Frame {
        override suspend fun getPreviewText(): String {
            return imageFrame.altText ?: getString(Res.string.image_span)
        }
    }

    @Serializable(OptionsSerializer::class)
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

private class TextSerializer : KSerializer<Frame.Text> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("frame_text") {
        element("id", serialDescriptor<Long>())
        element("content", serialDescriptor<String>())
    }

    override fun deserialize(decoder: Decoder): Frame.Text = decoder.decodeStructure(descriptor) {
        var id = -1L
        var content = ""
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> id = decodeLongElement(descriptor, index)
                1 -> content = decodeStringElement(descriptor, index)
            }
        }

        if (id < 0 || content.isEmpty()) {
            error("Missing id or content while deserializing Frame.Text")
        }

        return@decodeStructure Frame.Text(TextFrame(id, content))
    }

    override fun serialize(encoder: Encoder, value: Frame.Text) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.textFrame.id)
            encodeStringElement(descriptor, 1, value.textFrame.content)
        }
}

private class ImageSerializer : KSerializer<Frame.Image> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("frame_image") {
        element("id", serialDescriptor<Long>())
        element("filename", serialDescriptor<String>())
        element("width", serialDescriptor<Long>())
        element("height", serialDescriptor<Long>())
        element("alt_text", serialDescriptor<String>(), isOptional = true)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Frame.Image = decoder.decodeStructure(descriptor) {
        var id = -1L
        var filename = ""
        var width = 0L
        var height = 0L
        var altText: String? = null
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> id = decodeLongElement(descriptor, index)
                1 -> filename = decodeStringElement(descriptor, index)
                2 -> width = decodeLongElement(descriptor, index)
                3 -> height = decodeLongElement(descriptor, index)
                4 -> altText = decodeSerializableElement(descriptor, index, serializer<String>())
            }
        }
        if (id < 0 || width <= 0 || height <= 0) {
            error("Missing elements when decoding")
        }
        Frame.Image(
            ImageFrame(
                id, filename, width, height, altText
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Frame.Image) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.imageFrame.id)
            encodeStringElement(descriptor, 1, value.imageFrame.filename)
            encodeLongElement(descriptor, 2, value.imageFrame.width)
            encodeLongElement(descriptor, 3, value.imageFrame.height)
            if (value.imageFrame.altText != null) {
                encodeStringElement(descriptor, 4, value.imageFrame.altText)
            }
        }
}

private class OptionsSerializer : KSerializer<Frame.Options> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("frame_options") {
        element("id", serialDescriptor<Long>())
        element("name", serialDescriptor<String>(), isOptional = true)
        element("frames", serialDescriptor<Frame>(), isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Frame.Options = decoder.decodeStructure(descriptor) {
        var id = -1L
        var name: String? = null
        var frames = emptyList<Frame>()
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> id = decodeLongElement(descriptor, index)
                1 -> name = decodeStringElement(descriptor, index)
                2 -> frames = decodeSerializableElement(descriptor, index, serializer())
            }
        }
        Frame.Options(
            optionsFrame = OptionsFrame(id, name),
            frames = frames
        )
    }

    override fun serialize(encoder: Encoder, value: Frame.Options) = encoder.encodeStructure(descriptor) {
        encodeLongElement(descriptor, 0, value.optionsFrame.id)
        if (value.optionsFrame.name != null) {
            encodeStringElement(descriptor, 1, value.optionsFrame.name)
        }
        encodeSerializableElement(descriptor, 2, serializer(), value.frames)
    }
}
