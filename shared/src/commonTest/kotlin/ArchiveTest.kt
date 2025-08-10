import com.zhufucdev.practiso.datamodel.DimensionArchive
import com.zhufucdev.practiso.datamodel.FrameArchive
import com.zhufucdev.practiso.datamodel.QuizArchive
import com.zhufucdev.practiso.datamodel.archive
import com.zhufucdev.practiso.datamodel.unarchive
import com.zhufucdev.practiso.platform.randomUUID
import okio.Buffer
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class ArchiveTest {
    companion object {
        val sampleQuizSet = listOf(
            QuizArchive(
                name = "Test quiz 1",
                creationTime = Clock.System.now(),
                modificationTime = null,
                frames = listOf(
                    FrameArchive.Text(
                        content = "Hi I am text frame by test quiz 1"
                    ),
                    FrameArchive.Text(
                        content = "Hi I am another text frame by test quiz 1"
                    )
                ),
                dimensions = listOf(
                    DimensionArchive("test quiz", 1.0),
                    DimensionArchive("test item", 1.0)
                )
            ),
            QuizArchive(
                name = "Test quiz 2",
                creationTime = Clock.System.now() - 10.seconds,
                modificationTime = Clock.System.now(),
                frames = listOf(
                    FrameArchive.Text("Hi I am text frame by test quiz 2"),
                    FrameArchive.Image(
                        filename = "cat_walker.jpg",
                        width = 400,
                        height = 295,
                        altText = "The DJ Cat Walker popular among the Chinese"
                    ),
                    FrameArchive.Options(
                        name = "nice options",
                        content = listOf(
                            FrameArchive.Options.Item(
                                content = FrameArchive.Text("Option 1"),
                                isKey = true,
                                priority = 0
                            ),
                            FrameArchive.Options.Item(
                                content = FrameArchive.Text("Option 2"),
                                isKey = false,
                                priority = 0
                            )
                        )
                    ),
                    FrameArchive.Text("that's all")
                ),
            )
        )

        val fileSystem = FakeFileSystem()

        fun writeSampleQuizSet(fs: FileSystem, root: Path = "".toPath()): String {
            val name = randomUUID() + ".psarchive"
            GzipSink(fs.sink(root.resolve(name))).use { sink ->
                val buf = sampleQuizSet
                    .archive {
                        if (it == "cat_walker.jpg") {
                            Buffer()
                        } else {
                            error("Should not have reached here")
                        }
                    }
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
            val pack = source.gzip().buffer().unarchive()
            assertContentEquals(sampleQuizSet, pack.archives.quizzes)
        }
    }
}