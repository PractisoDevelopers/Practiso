package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.database.TextFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
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

@Serializable
sealed interface Frame {
    suspend fun getPreviewText(): String

    /**
     * The identifier of the frame in **context**
     * - Belonging to the quiz, it's the same as the underlying frame id
     * - Belonging to one of the [Options] within quiz, the value is its link id
     */
    val id: Long

    fun toArchive(): FrameArchive

    @Serializable(TextSerializer::class)
    data class Text(
        override val id: Long = -1,
        val textFrame: TextFrame = TextFrame(-1, ""),
    ) : Frame {
        override suspend fun getPreviewText(): String {
            return textFrame.content
        }

        override fun toArchive() = FrameArchive.Text(textFrame.content)
    }

    @Serializable(ImageSerializer::class)
    data class Image(
        override val id: Long = -1,
        val imageFrame: ImageFrame = ImageFrame(-1, "", 0, 0, null),
    ) : Frame {
        override suspend fun getPreviewText(): String {
            return imageFrame.altText ?: "🖼️"
        }

        override fun toArchive() = FrameArchive.Image(
            imageFrame.filename,
            imageFrame.width,
            imageFrame.height,
            imageFrame.altText
        )
    }

    sealed interface Answerable<T : PractisoAnswer> : Frame {
        fun List<T>.isAdequateNecessary(): Boolean
    }

    @Serializable(OptionsSerializer::class)
    data class Options(
        val optionsFrame: OptionsFrame = OptionsFrame(-1, null),
        val frames: List<KeyedPrioritizedFrame> = emptyList(),
    ) : Answerable<PractisoAnswer.Option> {
        override val id: Long
            get() = optionsFrame.id

        override fun List<PractisoAnswer.Option>.isAdequateNecessary(): Boolean {
            val optionIds = map(PractisoAnswer.Option::optionId)
            return frames.all { !it.isKey && it.frame.id !in optionIds || it.isKey && it.frame.id in optionIds }
        }

        override suspend fun getPreviewText(): String {
            return optionsFrame.name
                ?: frames.mapIndexed { index, frame -> "${'A' + index % 26}. ${frame.frame.getPreviewText()}" }
                    .joinToString(" ")
        }

        override fun toArchive() = FrameArchive.Options(
            optionsFrame.name,
            frames.map { FrameArchive.Options.Item(it.isKey, it.priority, it.frame.toArchive()) })
    }
}

@Serializable
data class KeyedPrioritizedFrame(val frame: Frame, val isKey: Boolean, val priority: Int)

@Serializable
data class PrioritizedFrame(val frame: Frame, val priority: Int)

@Serializable
data class QuizFrames(
    @Serializable(QuizSerializer::class) val quiz: Quiz,
    val frames: List<PrioritizedFrame>,
)

private suspend fun QuizQueries.getPrioritizedOptionsFrames(quizId: Long): List<PrioritizedFrame> =
    coroutineScope {
        getOptionsFrameByQuizId(quizId)
            .executeAsList()
            .map { optionsFrame ->
                async {
                    val textFrames =
                        getTextFrameByOptionsFrameId(optionsFrame.id) { id, content, linkId, isKey, priority ->
                            KeyedPrioritizedFrame(
                                frame = Frame.Text(linkId, TextFrame(id, content)),
                                isKey = isKey,
                                priority = priority.toInt()
                            )
                        }.executeAsList()

                    val imageFrames =
                        getImageFramesByOptionsFrameId(optionsFrame.id) { id, filename, width, height, altText, linkId, isKey, priority ->
                            KeyedPrioritizedFrame(
                                frame = Frame.Image(
                                    linkId,
                                    ImageFrame(
                                        id,
                                        filename,
                                        width,
                                        height,
                                        altText
                                    )
                                ),
                                isKey = isKey,
                                priority = priority.toInt()
                            )
                        }.executeAsList()

                    val frame = Frame.Options(
                        optionsFrame = OptionsFrame(optionsFrame.id, optionsFrame.name),
                        frames = (textFrames + imageFrames)
                            .sortedBy(KeyedPrioritizedFrame::priority)
                    )
                    PrioritizedFrame(frame, optionsFrame.priority.toInt())
                }
            }.awaitAll()
    }

private fun QuizQueries.getPrioritizedImageFrames(quizId: Long): List<PrioritizedFrame> =
    getImageFramesByQuizId(quizId) { id, filename, width, height, altText, priority ->
        PrioritizedFrame(
            frame = Frame.Image(id, ImageFrame(id, filename, width, height, altText)),
            priority = priority.toInt()
        )
    }
        .executeAsList()

private fun QuizQueries.getPrioritizedTextFrames(quizId: Long): List<PrioritizedFrame> =
    getTextFramesByQuizId(quizId) { id, content, priority ->
        PrioritizedFrame(
            frame = Frame.Text(id, TextFrame(id, content)),
            priority = priority.toInt()
        )
    }
        .executeAsList()

fun QuizQueries.getQuizFrames(starter: Query<Quiz>): Flow<List<QuizFrames>> =
    starter.asFlow()
        .mapToList(Dispatchers.IO)
        .distinctUntilChanged()
        .map { quizzes ->
            quizzes.map { quiz ->
                val frames =
                    coroutineScope {
                        listOf(
                            async { getPrioritizedOptionsFrames(quiz.id) },
                            async { getPrioritizedTextFrames(quiz.id) },
                            async { getPrioritizedImageFrames(quiz.id) }
                        )
                    }
                        .awaitAll()
                        .flatten()

                QuizFrames(
                    quiz = quiz,
                    frames = frames.sortedBy(PrioritizedFrame::priority)
                )
            }
        }

data class ResourceRequester(val name: String, val frame: Frame)

fun List<Frame>.resources(): List<ResourceRequester> = buildList {
    this@resources.forEach {
        when (it) {
            is Frame.Options -> {
                addAll(it.frames.map(KeyedPrioritizedFrame::frame).resources())
            }

            is Frame.Image -> {
                add(ResourceRequester(it.imageFrame.filename, it))
            }

            is Frame.Text -> {}
        }
    }
}

const val TextFrameSerialName = "text"
const val ImageFrameSerialName = "image"
const val OptionsFrameSerialName = "options"

private class TextSerializer : KSerializer<Frame.Text> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(TextFrameSerialName) {
        element("link_id", serialDescriptor<Long>())
        element("id", serialDescriptor<Long>())
        element("content", serialDescriptor<String>())
    }

    override fun deserialize(decoder: Decoder): Frame.Text = decoder.decodeStructure(descriptor) {
        var exId = -1L
        var id = -1L
        var content = ""
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> exId = decodeLongElement(descriptor, index)
                1 -> id = decodeLongElement(descriptor, index)
                2 -> content = decodeStringElement(descriptor, index)
            }
        }

        if (id < 0 || content.isEmpty()) {
            error("Missing id or content while deserializing Frame.Text")
        }

        return@decodeStructure Frame.Text(exId, TextFrame(id, content))
    }

    override fun serialize(encoder: Encoder, value: Frame.Text) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.id)
            encodeLongElement(descriptor, 1, value.textFrame.id)
            encodeStringElement(descriptor, 2, value.textFrame.content)
        }
}

private class ImageSerializer : KSerializer<Frame.Image> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ImageFrameSerialName) {
        element("link_id", serialDescriptor<Long>())
        element("id", serialDescriptor<Long>())
        element("embeddings_id", serialDescriptor<String?>())
        element("filename", serialDescriptor<String>())
        element("width", serialDescriptor<Long>())
        element("height", serialDescriptor<Long>())
        element("alt_text", serialDescriptor<String>(), isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Frame.Image = decoder.decodeStructure(descriptor) {
        var exId = -1L
        var id = -1L
        var filename = ""
        var width = 0L
        var height = 0L
        var altText: String? = null
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> exId = decodeLongElement(descriptor, index)
                1 -> id = decodeLongElement(descriptor, index)
                2 -> filename = decodeStringElement(descriptor, index)
                3 -> width = decodeLongElement(descriptor, index)
                4 -> height = decodeLongElement(descriptor, index)
                5 -> altText = decodeSerializableElement(descriptor, index, serializer<String>())
            }
        }
        if (id < 0 || width <= 0 || height <= 0) {
            error("Missing elements when decoding")
        }
        Frame.Image(
            exId,
            ImageFrame(
                id, filename, width, height, altText
            )
        )
    }

    override fun serialize(encoder: Encoder, value: Frame.Image) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.id)
            encodeLongElement(descriptor, 1, value.imageFrame.id)
            encodeStringElement(descriptor, 2, value.imageFrame.filename)
            encodeLongElement(descriptor, 3, value.imageFrame.width)
            encodeLongElement(descriptor, 4, value.imageFrame.height)
            if (value.imageFrame.altText != null) {
                encodeStringElement(descriptor, 5, value.imageFrame.altText)
            }
        }
}

private class OptionsSerializer : KSerializer<Frame.Options> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(OptionsFrameSerialName) {
        element("id", serialDescriptor<Long>())
        element("name", serialDescriptor<String>(), isOptional = true)
        element("frames", serialDescriptor<KeyedPrioritizedFrame>(), isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Frame.Options =
        decoder.decodeStructure(descriptor) {
            var id = -1L
            var name: String? = null
            var frames = emptyList<KeyedPrioritizedFrame>()
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

    override fun serialize(encoder: Encoder, value: Frame.Options) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.optionsFrame.id)
            if (value.optionsFrame.name != null) {
                encodeStringElement(descriptor, 1, value.optionsFrame.name)
            }
            encodeSerializableElement(descriptor, 2, serializer(), value.frames)
        }
}

private class QuizSerializer : KSerializer<Quiz> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("quiz") {
        element("id", serialDescriptor<Long>())
        element("name", serialDescriptor<String>())
        element("creationTime", serialDescriptor<Instant>())
        element("modificationTime", serialDescriptor<Instant>())
    }

    override fun deserialize(decoder: Decoder): Quiz = decoder.decodeStructure(descriptor) {
        var id = -1L
        var name: String? = null
        var creationTime = Instant.DISTANT_FUTURE
        var modificationTime: Instant? = null
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> id = decodeLongElement(descriptor, index)
                1 -> name = decodeStringElement(descriptor, index)
                2 -> creationTime = decodeSerializableElement(descriptor, index, serializer())
                3 -> modificationTime = decodeSerializableElement(descriptor, index, serializer())
            }
        }
        if (id < 0 || name == null || creationTime == Instant.DISTANT_FUTURE) {
            error("Missing elements when decoding")
        }
        Quiz(id, name, creationTime, modificationTime)
    }

    override fun serialize(encoder: Encoder, value: Quiz) = encoder.encodeStructure(descriptor) {
        encodeLongElement(descriptor, 0, value.id)
        encodeStringElement(descriptor, 1, value.name ?: "")
        encodeSerializableElement(descriptor, 2, serializer(), value.creationTimeISO)
        if (value.modificationTimeISO != null) {
            encodeSerializableElement(descriptor, 3, serializer(), value.modificationTimeISO)
        }
    }
}
