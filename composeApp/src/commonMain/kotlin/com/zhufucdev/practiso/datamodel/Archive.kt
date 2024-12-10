package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.TextFrame
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

fun Source.readArchive(): List<QuizArchive> = buildList {
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
        val resourceCount = buf.readInt()
        i = buf.indexOf(0)
        val resources: Map<String, () -> Source> = buildMap {
            repeat(resourceCount) {
                val resName = buf.readUtf8(i)
                buf.skip(1)
                val size = buf.readInt()
                val content = buf.readByteString(size.toLong())
                put(resName) { Buffer().write(content) }
                i = buf.indexOf(0)
            }
        }
        val frameXmlContent =
            if (i < 0) buf.readUtf8()
            else buf.readUtf8(i).also { buf.skip(1) }

        val frames: FrameContainer =
            xml.decodeFromString(serializer(), frameXmlContent)
        add(QuizArchive(name, creationTime, modTime, frames, resources))
        if (i < 0) {
            break
        }
    }
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
data class FrameContainer(val data: List<Frame> = emptyList())

class FrameContainerSerializer : KSerializer<FrameContainer> {
    private val elementSerializer by lazy { serializer<Frame>() }
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("frame_container") {
        element("data", ListSerializer(elementSerializer).descriptor)
    }

    override fun deserialize(decoder: Decoder): FrameContainer =
        if (decoder is XML.XmlInput) {
            decodeXml(decoder.input)
        } else {
            val data = decoder.decodeStructure(descriptor) {
                decodeSerializableElement(descriptor, 0, ListSerializer(elementSerializer))
            }
            FrameContainer(data)
        }

    private fun decodeTextFrame(reader: XmlReader): Frame.Text {
        reader.next()
        return Frame.Text(textFrame = TextFrame(-1, reader.text))
    }

    private fun decodeImageFrame(reader: XmlReader): Frame.Image {
        val width = reader.getAttributeValue(QName("width"))!!.toLong()
        val height = reader.getAttributeValue(QName("height"))!!.toLong()
        val alt = reader.getAttributeValue(QName("alt"))
        val src = reader.getAttributeValue(QName("src"))!!
        return Frame.Image(imageFrame = ImageFrame(-1, src, width, height, alt))
    }

    private fun decodeOptionsFrame(reader: XmlReader): Frame.Options {
        val name = reader.getAttributeValue(QName("name"))
        val frames = mutableListOf<KeyedPrioritizedFrame>()
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
                            frames.add(
                                KeyedPrioritizedFrame(
                                    decodeTextFrame(reader),
                                    isKey,
                                    priority
                                )
                            )
                        }

                        ImageFrameSerialName -> frames.add(
                            KeyedPrioritizedFrame(
                                decodeImageFrame(
                                    reader
                                ), isKey, priority
                            )
                        )

                        else -> error("Unsupported element ${reader.name.localPart}")
                    }
                }

                EventType.END_ELEMENT -> break
                else -> error("Unexpected tag ${tag.name}")
            }
        }
        return Frame.Options(optionsFrame = OptionsFrame(-1, name), frames)
    }

    private fun decodeXml(reader: XmlReader): FrameContainer {
        val frames = mutableListOf<Frame>()
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
        return FrameContainer(frames)
    }

    override fun serialize(encoder: Encoder, value: FrameContainer) {
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

    private fun encodeFrame(target: XmlWriter, frame: Frame) {
        when (frame) {
            is Frame.Options -> {
                target.startTag(null, OptionsFrameSerialName, null)
                if (frame.optionsFrame.name != null) {
                    target.writeAttribute("name", frame.optionsFrame.name)
                }
                frame.frames.forEach {
                    target.startTag(null, "option", null)
                    if (it.isKey) {
                        target.writeAttribute("key", true)
                    }
                    target.writeAttribute("priority", it.priority)
                    encodeFrame(target, it.frame)
                    target.endTag(null, "option", null)
                }
                target.endTag(null, OptionsFrameSerialName, null)
            }

            is Frame.Text -> {
                target.startTag(null, TextFrameSerialName, null)
                target.text(frame.textFrame.content)
                target.endTag(null, TextFrameSerialName, null)
            }

            is Frame.Image -> {
                target.startTag(null, ImageFrameSerialName, null)
                target.writeAttribute("width", frame.imageFrame.width)
                target.writeAttribute("height", frame.imageFrame.height)
                target.writeAttribute("src", frame.imageFrame.filename)
                if (frame.imageFrame.altText != null) {
                    target.writeAttribute("alt", frame.imageFrame.altText)
                }
                target.endTag(null, ImageFrameSerialName, null)
            }
        }
    }

    private fun encodeXml(target: XmlWriter, data: List<Frame>) {
        target.startTag(null, "frame_container", "")
        data.forEach {
            encodeFrame(target, it)
        }
        target.endTag(null, "frame_container", "")
    }
}


data class QuizArchive(
    val name: String,
    val creationTime: Instant,
    val modificationTime: Instant? = null,
    val frames: FrameContainer = FrameContainer(),
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
        val frames = xml.encodeToString(serializer(), a.frames).encodeToByteArray()
        write(frames)
        if (index < lastIndex) {
            writeByte(0)
        }
    }
}