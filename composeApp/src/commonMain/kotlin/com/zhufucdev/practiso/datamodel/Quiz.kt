package com.zhufucdev.practiso.datamodel

import androidx.compose.ui.util.fastForEachIndexed
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
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
    val id: Long

    fun insertInto(db: AppDatabase, quizId: Long, priority: Long)

    @Serializable(TextSerializer::class)
    data class Text(val textFrame: TextFrame = TextFrame(-1, "")) : Frame {
        override val id: Long
            get() = textFrame.id

        override suspend fun getPreviewText(): String {
            return textFrame.content
        }

        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            db.transaction {
                db.quizQueries.insertTextFrame(textFrame.content)
                db.quizQueries.associateLastTextFrameWithQuiz(quizId, priority)
            }
        }
    }

    @Serializable(ImageSerializer::class)
    data class Image(val imageFrame: ImageFrame = ImageFrame(-1, "", 0, 0, null)) : Frame {
        override val id: Long
            get() = imageFrame.id

        override suspend fun getPreviewText(): String {
            return imageFrame.altText ?: getString(Res.string.image_span)
        }

        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            db.transaction {
                insertTo(db)
                db.quizQueries.associateLastImageFrameWithQuiz(quizId, priority)
            }
        }
    }

    @Serializable(OptionsSerializer::class)
    data class Options(
        val optionsFrame: OptionsFrame = OptionsFrame(-1, null),
        val frames: List<KeyedPrioritizedFrame> = emptyList(),
    ) : Frame {
        override val id: Long
            get() = optionsFrame.id

        override suspend fun getPreviewText(): String {
            return optionsFrame.name
                ?: frames.mapIndexed { index, frame -> "${'A' + index % 26}. ${frame.frame.getPreviewText()}" }
                    .joinToString(" ")
        }

        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            val frameId = db.transactionWithResult {
                db.quizQueries.insertOptionsFrame(optionsFrame.name)
                val frameId = db.quizQueries.lastInsertRowId().executeAsOne()
                db.quizQueries.associateOptionsFrameWithQuiz(
                    quizId,
                    frameId,
                    priority
                )
                frameId
            }

            frames.forEach { optionFrame ->
                when (optionFrame.frame) {
                    is Image -> {
                        db.transaction {
                            optionFrame.frame.insertTo(db)
                            db.quizQueries.assoicateLastImageFrameWithOption(
                                frameId,
                                maxOf(optionFrame.priority, 0).toLong(),
                                optionFrame.isKey
                            )
                        }
                    }

                    is Text -> {
                        db.transaction {
                            db.quizQueries.insertTextFrame(optionFrame.frame.textFrame.content)
                            db.quizQueries.assoicateLastTextFrameWithOption(
                                frameId,
                                optionFrame.isKey,
                                maxOf(optionFrame.priority, 0).toLong()
                            )
                        }
                    }

                    is Options -> throw UnsupportedOperationException("Options frame inception")
                }
            }
        }
    }
}

@Serializable
data class KeyedPrioritizedFrame(val frame: Frame, val isKey: Boolean, val priority: Int)

@Serializable
data class PrioritizedFrame(val frame: Frame, val priority: Int)

data class QuizFrames(val quiz: Quiz, val frames: List<PrioritizedFrame>)

private suspend fun QuizQueries.getPrioritizedOptionsFrames(quizId: Long): List<PrioritizedFrame> =
    coroutineScope {
        getOptionsFrameByQuizId(quizId)
            .executeAsList()
            .map { optionsFrame ->
                async {
                    val textFrames =
                        getTextFrameByOptionsFrameId(optionsFrame.id) { id, content, isKey, priority ->
                            KeyedPrioritizedFrame(
                                frame = Frame.Text(TextFrame(id, content)),
                                isKey = isKey,
                                priority = priority.toInt()
                            )
                        }.executeAsList()

                    val imageFrames =
                        getImageFramesByOptionsFrameId(optionsFrame.id) { id, filename, width, height, altText, isKey, priority ->
                            KeyedPrioritizedFrame(
                                frame = Frame.Image(
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
            frame = Frame.Image(ImageFrame(id, filename, width, height, altText)),
            priority = priority.toInt()
        )
    }
        .executeAsList()

private fun QuizQueries.getPrioritizedTextFrames(quizId: Long): List<PrioritizedFrame> =
    getTextFramesByQuizId(quizId) { id, content, priority ->
        PrioritizedFrame(
            frame = Frame.Text(TextFrame(id, content)),
            priority = priority.toInt()
        )
    }
        .executeAsList()

fun QuizQueries.getQuizFrames(starter: Query<Quiz>): Flow<List<QuizFrames>> =
    starter.asFlow()
        .mapToList(Dispatchers.IO)
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

fun List<Frame>.insertInto(db: AppDatabase, name: String?) {
    val quizId = db.transactionWithResult {
        db.quizQueries.insertQuiz(name, Clock.System.now(), null)
        db.quizQueries.lastInsertRowId().executeAsOne()
    }

    fastForEachIndexed { index, frame ->
        frame.insertInto(db, quizId, index.toLong())
    }
}

fun Frame.Image.insertTo(db: AppDatabase) {
    db.quizQueries.insertImageFrame(
        imageFrame.filename,
        imageFrame.altText,
        imageFrame.width,
        imageFrame.height
    )
}
