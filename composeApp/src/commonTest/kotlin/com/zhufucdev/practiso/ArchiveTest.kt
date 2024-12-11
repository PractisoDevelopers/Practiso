package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.FrameArchive
import com.zhufucdev.practiso.datamodel.FrameArchiveContainer
import com.zhufucdev.practiso.datamodel.QuizArchive
import com.zhufucdev.practiso.datamodel.archive
import com.zhufucdev.practiso.datamodel.unarchive
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
import kotlin.test.assertContentEquals
import kotlin.time.Duration.Companion.seconds

class ArchiveTest {
    companion object {
        val sampleQuizSet = listOf(
            QuizArchive(
                name = "Test quiz 1",
                creationTime = Clock.System.now(),
                modificationTime = null,
                frames = FrameArchiveContainer(
                    listOf(
                        FrameArchive.Text(
                            content = "Hi I am text frame by test quiz 1"
                        )
                    )
                ),
            ),
            QuizArchive(
                name = "Test quiz 2",
                creationTime = Clock.System.now() - 10.seconds,
                modificationTime = Clock.System.now(),
                frames = FrameArchiveContainer(
                    listOf(
                        FrameArchive.Text(
                            content = "Hi I am text frame by test quiz 2"
                        ),
                        FrameArchive.Image(
                            filename = "cat_walker.jpg",
                            width = 400,
                            height = 295,
                            altText = "The DJ Cat Walker popular among the Chinese"
                        ),
                        FrameArchive.Options(
                            name = "nice options",
                            content = listOf(
                                FrameArchive.Option(
                                    content = FrameArchive.Text("Option 1"),
                                    isKey = true,
                                    priority = 0
                                ),
                                FrameArchive.Option(
                                    content = FrameArchive.Text("Option 2"),
                                    isKey = false,
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
            val archive = source.gzip().unarchive()
            assertContentEquals(sampleQuizSet, archive)
        }
    }
}