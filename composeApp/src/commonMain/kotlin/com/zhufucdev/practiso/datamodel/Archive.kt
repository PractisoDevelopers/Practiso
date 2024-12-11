package com.zhufucdev.practiso.datamodel

import androidx.compose.ui.util.fastForEachIndexed
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.readDouble
import com.zhufucdev.practiso.writeDouble
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.writeAttribute
import okio.Buffer
import okio.Sink
import okio.Source
import okio.buffer

private val xml = XML {
    defaultPolicy {
        isStrictBoolean = true
        autoPolymorphic = true
    }

    xmlVersion = XmlVersion.XML11
    isInlineCollapsed = true
    indent = 4
}

fun QuizArchive.importTo(db: AppDatabase, resourceSink: (String) -> Sink) {
    val quizId = db.transactionWithResult {
        db.quizQueries.insertQuiz(name, creationTime, null)
        db.quizQueries.lastInsertRowId().executeAsOne()
    }

    frames.data.forEachIndexed { index, frame ->
        frame.insertInto(db, quizId, index.toLong())
    }

    resources.forEach { (name, resource) ->
        resource().buffer().readAll(resourceSink(name))
    }
}

@Serializable(FrameContainerSerializer::class)
data class FrameArchiveContainer(val data: List<FrameArchive> = emptyList())

class FrameContainerSerializer : KSerializer<FrameArchiveContainer> {
    private val elementSerializer by lazy { serializer<FrameArchive>() }
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("frame_container") {
        element("data", ListSerializer(elementSerializer).descriptor)
    }

    override fun deserialize(decoder: Decoder): FrameArchiveContainer =
        if (decoder is XML.XmlInput) {
            decodeXml(decoder.input)
        } else {
            val data = decoder.decodeStructure(descriptor) {
                decodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer))
            }
            FrameArchiveContainer(data)
        }

    private fun decodeTextFrame(reader: XmlReader): FrameArchive.Text {
        reader.next()
        return FrameArchive.Text(reader.text)
    }

    private fun decodeImageFrame(reader: XmlReader): FrameArchive.Image {
        val width = reader.getAttributeValue(QName("width"))!!.toLong()
        val height = reader.getAttributeValue(QName("height"))!!.toLong()
        val alt = reader.getAttributeValue(QName("alt"))
        val src = reader.getAttributeValue(QName("src"))!!
        return FrameArchive.Image(src, width, height, alt)
    }

    private fun decodeOptionsFrame(reader: XmlReader): FrameArchive.Options {
        val name = reader.getAttributeValue(QName("name"))
        val options = mutableListOf<FrameArchive.Option>()
        while (reader.hasNext()) {
            reader.nextTag()
            if (reader.name.localPart != "option") {
                error("Unexpected tag ${reader.name.localPart} within an options frame")
            }
            val isKey = reader.getAttributeValue(QName("key")) != null
            val priority = reader.getAttributeValue(QName("priority"))!!.toInt()

            when (val tag = reader.nextTag()) {
                EventType.START_ELEMENT -> {
                    when (reader.name.localPart) {
                        TextFrameSerialName -> {
                            options.add(
                                FrameArchive.Option(
                                    isKey,
                                    priority,
                                    decodeTextFrame(reader)
                                )
                            )
                        }

                        ImageFrameSerialName -> options.add(
                            FrameArchive.Option(
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
        }
        return FrameArchive.Options(name, options)
    }

    private fun decodeXml(reader: XmlReader): FrameArchiveContainer {
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
        return FrameArchiveContainer(frames)
    }

    override fun serialize(encoder: Encoder, value: FrameArchiveContainer) {
        if (encoder is XML.XmlOutput) {
            encodeXml(encoder.target, value.data)
        } else {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(
                    descriptor,
                    0,
                    ListSerializer(elementSerializer),
                    value.data
                )
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
                    target.startTag(null, "option", null)
                    if (it.isKey) {
                        target.writeAttribute("key", true)
                    }
                    target.writeAttribute("priority", it.priority)
                    encodeFrame(target, it.content)
                    target.endTag(null, "option", null)
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
        target.startTag(null, "frame_container", "")
        data.forEach {
            encodeFrame(target, it)
        }
        target.endTag(null, "frame_container", "")
    }
}

fun FrameArchive.Image.insertTo(db: AppDatabase) {
    db.quizQueries.insertImageFrame(filename, altText, width, height)
}

@Serializable
sealed interface FrameArchive {
    fun insertInto(db: AppDatabase, quizId: Long, priority: Long)

    @Serializable
    data class Text(val content: String) : FrameArchive {
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
    data class Option(val isKey: Boolean, val priority: Int, val content: FrameArchive)

    @Serializable
    data class Options(val name: String?, val content: List<Option>) : FrameArchive {
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

data class QuizArchive(
    val name: String,
    val creationTime: Instant,
    val modificationTime: Instant? = null,
    val frames: FrameArchiveContainer = FrameArchiveContainer(),
    val dimensions: List<DimensionIntensity> = emptyList(),
    val resources: Map<String, () -> Source> = emptyMap(),
)

fun List<QuizArchive>.archive(): Source = Buffer().apply {
    forEachIndexed { index, a ->
        writeUtf8(a.name)
        writeByte(0)
        writeUtf8(a.creationTime.toString())
        writeByte(0)
        if (a.modificationTime != null) {
            writeUtf8(a.modificationTime.toString())
        }
        writeByte(0)
        writeInt(a.resources.size)
        a.resources.forEach { (name, resource) ->
            writeUtf8(name)
            writeByte(0)
            val bs = resource().buffer().readByteString()
            writeInt(bs.size)
            write(bs)
        }
        writeInt(a.dimensions.size)
        a.dimensions.forEach {
            writeUtf8(it.dimension.name)
            writeByte(0)
            writeDouble(it.intensity)
        }
        val frames = xml.encodeToString(serializer(), a.frames).encodeToByteArray()
        write(frames)
        if (index < lastIndex) {
            writeByte(0)
        }
    }
}

fun Source.unarchive(): List<QuizArchive> = buildList {
    val buf = buffer()
    while (!buf.exhausted()) {
        var i = buf.indexOf(0)
        val name = buf.readUtf8(i)
        buf.skip(1)
        i = buf.indexOf(0)
        val creationTime = Instant.parse(buf.readUtf8(i))
        buf.skip(1)
        i = buf.indexOf(0)
        val modTime = if (i > 0) Instant.parse(buf.readUtf8(i)) else null
        buf.skip(1)
        val resources: Map<String, () -> Source> = buildMap {
            val resourceCount = buf.readInt()
            repeat(resourceCount) {
                i = buf.indexOf(0)
                val resName = buf.readUtf8(i)
                buf.skip(1)
                val size = buf.readInt()
                val content = buf.readByteString(size.toLong())
                put(resName) { Buffer().write(content) }
            }
        }
        val dimensions: List<DimensionIntensity> = buildList {
            val dimensionCount = buf.readInt()
            repeat(dimensionCount) {
                i = buf.indexOf(0)
                val dimenName = buf.readUtf8(i)
                buf.skip(0)
                val intensity = buf.readDouble()
                add(DimensionIntensity(Dimension(-1, dimenName), intensity))
            }
        }

        i = buf.indexOf(0)
        val frameXmlContent =
            if (i < 0) buf.readUtf8()
            else buf.readUtf8(i).also { buf.skip(1) }

        val frames: FrameArchiveContainer =
            xml.decodeFromString(serializer(), frameXmlContent)
        add(QuizArchive(name, creationTime, modTime, frames, dimensions, resources))
        if (i < 0) {
            break
        }
    }
}
