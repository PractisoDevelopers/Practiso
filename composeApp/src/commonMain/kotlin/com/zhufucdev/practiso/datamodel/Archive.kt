package com.zhufucdev.practiso.datamodel

import androidx.compose.ui.util.fastForEachIndexed
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.writeAttribute
import okio.Buffer
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.buffer

private val xml = XML {
    recommended_0_90_2()
}

fun QuizArchive.importTo(db: AppDatabase) {
    val quizId = db.transactionWithResult {
        db.quizQueries.insertQuiz(name, creationTime, null)
        db.quizQueries.lastInsertRowId().executeAsOne()
    }

    frames.forEachIndexed { index, frame ->
        frame.insertInto(db, quizId, index.toLong())
    }
}

fun ArchivePack.importAll(db: AppDatabase, resourceSink: (String) -> Sink) {
    resources.forEach { (name, sink) ->
        sink().buffer().readAll(resourceSink(name))
    }
    archives.quizzes.forEach { it.importTo(db) }
}

class FrameContainerSerializer : KSerializer<List<FrameArchive>> {
    private val delegateSerializer = ListSerializer(serializer<FrameArchive>())

    companion object {
        const val SERIAL_NAME = "frames"
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(SERIAL_NAME) {
        element("data", delegateSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): List<FrameArchive> =
        if (decoder is XML.XmlInput) {
            val r = decodeXml(decoder.input)
            r
        } else {
            decoder.decodeStructure(descriptor) {
                decodeSerializableElement(descriptor, 0, delegateSerializer)
            }
        }

    private fun decodeTextFrame(reader: XmlReader): FrameArchive.Text {
        try {
            val tag = reader.next()
            if (tag != EventType.TEXT) {
                error("Unexpected tag in a text frame: ${tag.name}")
            }
            return FrameArchive.Text(reader.text)
        } finally {
            val tag = reader.next()
            if (tag != EventType.END_ELEMENT) {
                error("Unexpected tag at the end of text frame: ${tag.name}")
            }
        }
    }

    private fun decodeImageFrame(reader: XmlReader): FrameArchive.Image {
        val width = reader.getAttributeValue(QName("width"))!!.toLong()
        val height = reader.getAttributeValue(QName("height"))!!.toLong()
        val alt = reader.getAttributeValue(QName("alt"))
        val src = reader.getAttributeValue(QName("src"))!!
        try {
            return FrameArchive.Image(src, width, height, alt)
        } finally {
            val tag = reader.next()
            if (tag != EventType.END_ELEMENT) {
                error("Unexpected tag at the end of image frame: ${tag.name}")
            }
        }
    }

    private fun decodeOptionsFrame(reader: XmlReader): FrameArchive.Options {
        val name = reader.getAttributeValue(QName("name"))
        val options = mutableListOf<FrameArchive.Options.Item>()
        while (reader.hasNext()) {
            val startOrEndEvent = reader.nextTag()
            when (startOrEndEvent) {
                EventType.END_ELEMENT -> break
                EventType.START_ELEMENT ->
                    if (reader.name.localPart != "item") {
                        error("Unexpected tag ${reader.name.localPart} at the start of an options frame item")
                    }
                else -> error("Unexpected tag at the start of an options frame item")
            }

            val isKey = reader.getAttributeValue(QName("key")) != null
            val priority = reader.getAttributeValue(QName("priority"))!!.toInt()

            when (val tag = reader.nextTag()) {
                EventType.START_ELEMENT -> {
                    when (reader.name.localPart) {
                        TextFrameSerialName -> {
                            options.add(
                                FrameArchive.Options.Item(
                                    isKey,
                                    priority,
                                    decodeTextFrame(reader)
                                )
                            )
                        }

                        ImageFrameSerialName -> options.add(
                            FrameArchive.Options.Item(
                                isKey, priority,
                                decodeImageFrame(
                                    reader
                                )
                            )
                        )

                        else -> error("Unsupported element ${reader.name.localPart}")
                    }
                }

                EventType.END_ELEMENT -> break
                else -> error("Unexpected tag ${tag.name}")
            }

            var endEvent = reader.next()
            while (endEvent != EventType.END_ELEMENT) {
                if (endEvent != EventType.IGNORABLE_WHITESPACE) {
                    error("Unexpected ${endEvent.name} at the end of option item")
                }
                endEvent = reader.next()
            }
        }
        return FrameArchive.Options(name, options)
    }

    private fun decodeXml(reader: XmlReader): List<FrameArchive> {
        val frames = mutableListOf<FrameArchive>()
        while (reader.hasNext()) {
            when (reader.nextTag()) {
                EventType.START_ELEMENT -> {
                    when (reader.name.getLocalPart()) {
                        TextFrameSerialName -> frames.add(decodeTextFrame(reader))
                        ImageFrameSerialName -> frames.add(decodeImageFrame(reader))
                        OptionsFrameSerialName -> frames.add(decodeOptionsFrame(reader))
                    }
                }

                EventType.END_ELEMENT -> break
                EventType.COMMENT,
                EventType.IGNORABLE_WHITESPACE,
                    -> continue

                EventType.TEXT -> error("Unexpected text")
                EventType.CDSECT -> error("Unexpected CDSECT")
                EventType.DOCDECL -> error("Unexpected DOCDECL")
                EventType.END_DOCUMENT -> error("Unexpected end of document")
                EventType.ENTITY_REF -> error("Unexpected entity reference")
                EventType.START_DOCUMENT -> error("Unexpected begin of document")
                EventType.ATTRIBUTE -> error("Unexpected attribute")
                EventType.PROCESSING_INSTRUCTION -> error("Unexpected processing instruction")
            }
        }
        return frames
    }

    override fun serialize(encoder: Encoder, value: List<FrameArchive>) {
        if (encoder is XML.XmlOutput) {
            encodeXml(encoder.target, value)
        } else {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(descriptor, 0, delegateSerializer, value)
            }
        }
    }

    private fun encodeFrame(target: XmlWriter, frame: FrameArchive) {
        when (frame) {
            is FrameArchive.Options -> {
                target.startTag(null, OptionsFrameSerialName, null)
                if (frame.name != null) {
                    target.writeAttribute("name", frame.name)
                }
                frame.content.forEach {
                    target.startTag(null, "item", null)
                    if (it.isKey) {
                        target.writeAttribute("key", true)
                    }
                    target.writeAttribute("priority", it.priority)
                    encodeFrame(target, it.content)
                    target.endTag(null, "item", null)
                }
                target.endTag(null, OptionsFrameSerialName, null)
            }

            is FrameArchive.Text -> {
                target.startTag(null, TextFrameSerialName, null)
                target.text(frame.content)
                target.endTag(null, TextFrameSerialName, null)
            }

            is FrameArchive.Image -> {
                target.startTag(null, ImageFrameSerialName, null)
                target.writeAttribute("width", frame.width)
                target.writeAttribute("height", frame.height)
                target.writeAttribute("src", frame.filename)
                if (frame.altText != null) {
                    target.writeAttribute("alt", frame.altText)
                }
                target.endTag(null, ImageFrameSerialName, null)
            }
        }
    }

    private fun encodeXml(target: XmlWriter, data: List<FrameArchive>) {
        target.startTag(null, SERIAL_NAME, null)
        data.forEach {
            encodeFrame(target, it)
        }
        target.endTag(null, SERIAL_NAME, null)
    }
}

fun FrameArchive.Image.insertTo(db: AppDatabase) {
    db.quizQueries.insertImageFrame(filename, altText, width, height)
}

@Serializable
sealed interface FrameArchive {
    fun insertInto(db: AppDatabase, quizId: Long, priority: Long)

    @Serializable
    data class Text(@XmlValue val content: String) : FrameArchive {
        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            db.transaction {
                db.quizQueries.insertTextFrame(content)
                db.quizQueries.associateLastTextFrameWithQuiz(quizId, priority)
            }
        }
    }

    @Serializable
    data class Image(
        val filename: String,
        val width: Long,
        val height: Long,
        val altText: String?,
    ) : FrameArchive {
        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            db.transaction {
                insertTo(db)
                db.quizQueries.associateLastImageFrameWithQuiz(quizId, priority)
            }
        }
    }

    @Serializable
    data class Options(val name: String?, @XmlValue val content: List<Item>) : FrameArchive {
        @Serializable
        data class Item(val isKey: Boolean, val priority: Int, @XmlValue val content: FrameArchive)

        override fun insertInto(db: AppDatabase, quizId: Long, priority: Long) {
            val frameId = db.transactionWithResult {
                db.quizQueries.insertOptionsFrame(name)
                val frameId = db.quizQueries.lastInsertRowId().executeAsOne()
                db.quizQueries.associateOptionsFrameWithQuiz(
                    quizId,
                    frameId,
                    priority
                )
                frameId
            }

            content.forEach { option ->
                when (option.content) {
                    is Image -> {
                        db.transaction {
                            option.content.insertTo(db)
                            db.quizQueries.assoicateLastImageFrameWithOption(
                                frameId,
                                maxOf(option.priority, 0).toLong(),
                                option.isKey
                            )
                        }
                    }

                    is Text -> {
                        db.transaction {
                            db.quizQueries.insertTextFrame(option.content.content)
                            db.quizQueries.assoicateLastTextFrameWithOption(
                                frameId,
                                option.isKey,
                                maxOf(option.priority, 0).toLong()
                            )
                        }
                    }

                    is Options -> throw UnsupportedOperationException("Options frame inception")
                }
            }
        }
    }
}

fun List<FrameArchive>.insertInto(db: AppDatabase, name: String?) {
    val quizId = db.transactionWithResult {
        db.quizQueries.insertQuiz(name, Clock.System.now(), null)
        db.quizQueries.lastInsertRowId().executeAsOne()
    }

    fastForEachIndexed { index, frame ->
        frame.insertInto(db, quizId, index.toLong())
    }
}

@Serializable
@XmlSerialName("dimension")
data class DimensionArchive(val name: String, @XmlValue val intensity: Double)

@Serializable
@XmlSerialName("quiz")
data class QuizArchive(
    val name: String,
    @XmlSerialName("creation")
    val creationTime: Instant,
    @XmlSerialName("modification")
    val modificationTime: Instant? = null,
    @Serializable(FrameContainerSerializer::class)
    val frames: List<FrameArchive> = emptyList(),
    val dimensions: List<DimensionArchive> = emptyList(),
)

@Serializable
@XmlSerialName(value = "archive", namespace = "http://schema.zhufucdev.com/practiso")
data class QuizArchiveContainer(
    @XmlSerialName("creation")
    val creationTime: Instant,
    @XmlValue
    val quizzes: List<QuizArchive>,
)

data class ArchivePack(
    val archives: QuizArchiveContainer,
    val resources: Map<String, () -> Source>,
)

fun <T : FrameArchive> List<T>.resources(): List<String> = buildList {
    this@resources.forEach {
        when (it) {
            is FrameArchive.Image -> add(it.filename)
            is FrameArchive.Options -> addAll(
                it.content.map(FrameArchive.Options.Item::content).resources()
            )

            is FrameArchive.Text -> {}
        }
    }
}

fun List<QuizArchive>.archive(resourceSource: (String) -> Source): Source = Buffer().apply {
    val container = QuizArchiveContainer(Clock.System.now(), this@archive)
    val xmlContent = xml.encodeToString(container)
    writeUtf8(xmlContent)
    writeByte(0)
    this@archive
        .map(QuizArchive::frames)
        .map { it.resources().map { n -> n to resourceSource(n) } }
        .flatten()
        .forEach { (name, source) ->
            writeUtf8(name)
            writeByte(0)
            val bs = source.buffer().readByteString()
            writeInt(bs.size)
            write(bs)
        }
}

fun BufferedSource.unarchive(): ArchivePack {
    var i = indexOf(0)
    val xmlText = readUtf8(i)
    skip(1)

    val archives: QuizArchiveContainer = xml.decodeFromString(serializer(), xmlText)
    val resourcePool: Map<String, () -> Source> = buildMap {
        while (!exhausted()) {
            i = indexOf(0)
            val name = readUtf8(i)
            skip(0)
            val bs = readByteString()
            put(name) { Buffer().write(bs) }
        }
    }
    return ArchivePack(archives, resourcePool)
}
