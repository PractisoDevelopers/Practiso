package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.ArchivePack
import com.zhufucdev.practiso.datamodel.ErrorMessage
import com.zhufucdev.practiso.datamodel.ErrorModel
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.datamodel.importTo
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.unarchive
import com.zhufucdev.practiso.helper.resourceSink
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.platform.randomUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okio.IOException
import okio.buffer
import okio.gzip
import okio.use
import kotlin.coroutines.cancellation.CancellationException

sealed interface ImportState {
    data class Idle(val reason: IdleReason) : ImportState
    data class Unarchiving(val target: String) : ImportState
    data class Confirmation(
        val total: Int,
        val ok: SendChannel<Unit>,
        val dismiss: SendChannel<Unit>,
    ) : ImportState

    data class Importing(val total: Int, val done: Int) : ImportState

    data class Error(
        val model: ErrorModel,
        val cancel: SendChannel<Unit>,
        val retry: SendChannel<Unit>? = null,
        val skip: SendChannel<Unit>? = null,
        val ignore: SendChannel<Unit>? = null,
    ) : ImportState

    enum class IdleReason {
        Initialization, Completion, Cancellation
    }
}

class ImportService(private val db: AppDatabase = Database.app) {
    @Throws(IOException::class)
    fun unarchive(it: NamedSource): ArchivePack =
        it.source.gzip().buffer().unarchive()

    fun import(pack: ArchivePack) = channelFlow {
        val cancelChannel = Channel<Unit>()

        Channel<Unit>().let { continueChannel ->
            send(
                ImportState.Confirmation(
                    total = pack.archives.quizzes.size,
                    dismiss = cancelChannel,
                    ok = continueChannel
                )
            )

            if (!select {
                    continueChannel.onReceive {
                        true
                    }

                    cancelChannel.onReceive {
                        false
                    }
                }
            ) {
                send(ImportState.Idle(ImportState.IdleReason.Cancellation))
                return@channelFlow
            }
        }

        withContext(Dispatchers.IO) {
            for (index in pack.archives.quizzes.indices) {
                val quizArchive = pack.archives.quizzes[index]
                send(
                    ImportState.Importing(
                        total = pack.archives.quizzes.size,
                        done = index
                    )
                )

                var shouldReturn = false

                val importedQuiz = quizArchive.importTo(db)
                val resources = quizArchive.frames.resources()
                val platform = getPlatform()

                suspend fun rollback(resourcesIndicates: IntRange) {
                    db.transaction {
                        db.quizQueries.removeQuizzesWithFrames(listOf(importedQuiz))
                    }
                    resources.slice(resourcesIndicates).forEach { (name) ->
                        platform.filesystem.delete(platform.resourcePath.resolve(name))
                    }
                }

                for (i in resources.indices) {
                    val (name, requester) = resources[i]

                    var shouldBreak = false
                    val source = pack.resources[name]
                    if (source == null) {
                        val skipChannel = Channel<Unit>()
                        val ignoreChannel = Channel<Unit>()
                        send(
                            ImportState.Error(
                                model = ErrorModel(
                                    scope = AppScope.LibraryIntentModel,
                                    message = ErrorMessage.CopyResource(
                                        requester.name ?: name,
                                        quizArchive.name
                                    )
                                ),
                                cancel = cancelChannel,
                                skip = skipChannel,
                                ignore = ignoreChannel
                            )
                        )

                        select<Unit> {
                            skipChannel.onReceive {
                                rollback(0..i)
                                shouldBreak = true
                            }

                            cancelChannel.onReceive {
                                rollback(0..i)
                                shouldReturn = true
                            }

                            ignoreChannel.onReceive {
                            }
                        }
                    } else {
                        val skipChannel = Channel<Unit>()
                        val ignoreChannel = Channel<Unit>()
                        try {
                            platform.filesystem
                                .sink(platform.resourcePath.resolve(name))
                                .buffer()
                                .use { b ->
                                    b.writeAll(source())
                                }
                        } catch (e: Exception) {
                            send(
                                ImportState.Error(
                                    model = ErrorModel(
                                        scope = AppScope.LibraryIntentModel,
                                        cause = e,
                                        message = ErrorMessage.CopyResource(
                                            requester.name ?: name,
                                            quizArchive.name
                                        )
                                    ),
                                    cancel = cancelChannel,
                                    skip = skipChannel,
                                    ignore = ignoreChannel
                                )
                            )

                            select<Unit> {
                                skipChannel.onReceive {
                                    rollback(0..i)
                                    shouldBreak = true
                                }

                                ignoreChannel.onReceive {
                                }

                                cancelChannel.onReceive {
                                    rollback(0..i)
                                    shouldReturn = true
                                }
                            }
                        }
                    }

                    if (shouldReturn || shouldBreak) {
                        break
                    }
                }

                if (shouldReturn) {
                    return@withContext
                }
            }
        }

        send(ImportState.Idle(ImportState.IdleReason.Completion))
    }

    fun import(namedSource: NamedSource): Flow<ImportState> = channelFlow {
        send(ImportState.Unarchiving(namedSource.name))
        val cancelChannel = Channel<Unit>()
        val pack = withContext(Dispatchers.IO) {
            try {
                unarchive(namedSource)
            } catch (e: Exception) {
                send(
                    ImportState.Error(
                        model = ErrorModel(
                            scope = AppScope.LibraryIntentModel,
                            cause = e,
                            message = ErrorMessage.InvalidFileFormat
                        ),
                        cancel = cancelChannel
                    )
                )
                e.printStackTrace()
                null
            }
        }
        if (pack == null) {
            cancelChannel.receive()
            send(ImportState.Idle(ImportState.IdleReason.Cancellation))
            return@channelFlow
        }

        import(pack).collect {
            send(it)
        }
    }

    fun importImage(namedSource: NamedSource): String {
        val name = randomUUID() + "." + namedSource.name.split(".").last()
        val platform = getPlatform()
        namedSource.source.buffer().readAll(
            platform.resourceSink(name)
        )
        return name
    }

    /**
     * Import an [NamedSource] which represents only one
     * [com.zhufucdev.practiso.datamodel.QuizArchive],
     * throwing [ArchiveAssertionException] if there's more
     * than one quiz or [EmptyArchiveException] otherwise.
     *
     * If any of the required resources is not present,
     * throws [ResourceNotFoundException].
     *
     * @return id of the imported quiz.
     */
    @Throws(AssertionError::class, ResourceNotFoundException::class, CancellationException::class)
    suspend fun importSingleton(namedSource: NamedSource): Long {
        val unpack = unarchive(namedSource)
        if (unpack.archives.quizzes.size > 1) {
            throw ArchiveAssertionException()
        } else if (unpack.archives.quizzes.isEmpty()) {
            throw EmptyArchiveException()
        }

        val quiz = unpack.archives.quizzes.first()
        val resources = quiz.frames.resources()
        resources.forEach {
            if (!unpack.resources.containsKey(it.name)) {
                throw ResourceNotFoundException(it.name)
            }
        }
        val platform = getPlatform()
        resources.forEach {
            unpack.resources[it.name]!!.invoke()
                .buffer()
                .readAll(
                    platform.filesystem.sink(platform.resourcePath.resolve(it.name))
                )
        }
        return quiz.importTo(db)
    }
}

class ResourceNotFoundException(val resourceName: String) :
    Exception("Resource \"${resourceName}\" is absent.")

class EmptyArchiveException : Exception()

class ArchiveAssertionException : Exception()