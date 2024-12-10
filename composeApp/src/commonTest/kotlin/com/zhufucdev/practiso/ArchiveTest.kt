package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.FrameContainer
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import com.zhufucdev.practiso.datamodel.QuizArchive
import com.zhufucdev.practiso.datamodel.archive
import com.zhufucdev.practiso.datamodel.readArchive
import com.zhufucdev.practiso.platform.randomUUID
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.GzipSink
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.gzip
import okio.use
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

class ArchiveTest {
    companion object {
        val sampleQuizSet = listOf(
            QuizArchive(
                name = "Test quiz 1",
                creationTime = Clock.System.now(),
                modificationTime = null,
                frames = FrameContainer(
                    listOf(
                        Frame.Text(
                            id = 1,
                            textFrame = TextFrame(
                                id = 1,
                                content = "Hi I am text frame by test quiz 1"
                            )
                        )
                    )
                ),
            ),
            QuizArchive(
                name = "Test quiz 2",
                creationTime = Clock.System.now() - 10.seconds,
                modificationTime = Clock.System.now(),
                frames = FrameContainer(
                    listOf(
                        Frame.Text(
                            id = 1,
                            textFrame = TextFrame(
                                id = 1,
                                content = "Hi I am text frame by test quiz 2"
                            )
                        ),
                        Frame.Image(
                            id = 2,
                            imageFrame = ImageFrame(
                                id = 2,
                                filename = "cat_walker.jpg",
                                width = 400,
                                height = 295,
                                altText = "The DJ Cat Walker popular among the Chinese"
                            )
                        ),
                        Frame.Options(
                            optionsFrame = OptionsFrame(3, null),
                            frames = listOf(
                                KeyedPrioritizedFrame(
                                    frame = Frame.Text(1, TextFrame(4, "Option 1")),
                                    isKey = true,
                                    priority = 0
                                )
                            )
                        )
                    )
                ),
            )
        )

        val fileSystem = FakeFileSystem()
        fun writeSampleQuizSet(fs: FileSystem, root: Path = "".toPath()): String {
            val name = randomUUID() + ".psarchive"
            GzipSink(fs.sink(root.resolve(name))).use { sink ->
                val buf = sampleQuizSet
                    .archive()
                    .buffer()
                buf.readAll(sink)
            }

            return name
        }
    }

    val name = writeSampleQuizSet(fileSystem)

    @Test
    fun shouldRead() {
        fileSystem.source(name.toPath()).use { source ->
            val archive = source.gzip().readArchive()
            assertSame(sampleQuizSet.size, archive.size, "size")
        }
    }
}