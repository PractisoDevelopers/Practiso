package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.ArchivePack
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.datamodel.importTo
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.unarchive
import com.zhufucdev.practiso.helper.copyTo
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.platform.randomUUID
import com.zhufucdev.practiso.viewmodel.AppScope
import com.zhufucdev.practiso.viewmodel.ErrorMessage
import com.zhufucdev.practiso.viewmodel.ErrorModel
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
import resources.Res
import resources.failed_to_copy_resource_x_for_quiz_y_para
import resources.invalid_file_format_para
import resources.resource_x_for_quiz_y_was_not_found_para

sealed interface ImportState {
    data object Idle : ImportState
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
}

class ImportService(private val db: AppDatabase = Database.app) {
    @Throws(IOException::class)
    fun unarchive(it: Importable): ArchivePack =
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
                send(ImportState.Idle)
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

                var shouldBreak = false

                db.transaction {
                    quizArchive.importTo(db)
                    val resources = quizArchive.frames.resources().toList()
                    val platform = getPlatform()
                    resources.forEachIndexed { i, (name, requester) ->
                        val source = pack.resources[name]
                        if (source == null) {
                            val skipChannel = Channel<Unit>()
                            val ignoreChannel = Channel<Unit>()
                            send(
                                ImportState.Error(
                                    model = ErrorModel(
                                        scope = AppScope.LibraryIntentModel,
                                        message = ErrorMessage.Localized(
                                            resource = Res.string.resource_x_for_quiz_y_was_not_found_para,
                                            args = listOf(
                                                requester.name ?: name,
                                                quizArchive.name
                                            )
                                        )
                                    ),
                                    cancel = cancelChannel,
                                    skip = skipChannel,
                                    ignore = ignoreChannel
                                )
                            )

                            select<Unit> {
                                skipChannel.onReceive {
                                    resources.subList(0, i).forEach { (name) ->
                                        platform.filesystem
                                            .delete(platform.resourcePath.resolve(name))
                                    }
                                    rollback()
                                }

                                cancelChannel.onReceive {
                                    shouldBreak = true
                                    rollback()
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
                                            exception = e,
                                            message = ErrorMessage.Localized(
                                                resource = Res.string.failed_to_copy_resource_x_for_quiz_y_para,
                                                args = listOf(
                                                    requester.name ?: name,
                                                    quizArchive.name
                                                )
                                            )
                                        ),
                                        cancel = cancelChannel,
                                        skip = skipChannel,
                                        ignore = ignoreChannel
                                    )
                                )

                                select<Unit> {
                                    skipChannel.onReceive {
                                        resources.subList(0, i).forEach { (name) ->
                                            platform.filesystem
                                                .delete(
                                                    platform.resourcePath.resolve(
                                                        name
                                                    )
                                                )
                                        }
                                        rollback()
                                    }

                                    ignoreChannel.onReceive {
                                    }

                                    cancelChannel.onReceive {
                                        shouldBreak = true
                                        rollback()
                                    }
                                }
                            }
                        }
                    }
                }

                if (shouldBreak) {
                    break
                }
            }
        }

        send(ImportState.Idle)
    }

    fun import(importable: Importable): Flow<ImportState> = channelFlow {
        send(ImportState.Unarchiving(importable.name))
        val cancelChannel = Channel<Unit>()
        val pack = withContext(Dispatchers.IO) {
            try {
                unarchive(importable)
            } catch (e: Exception) {
                send(
                    ImportState.Error(
                        model = ErrorModel(
                            scope = AppScope.LibraryIntentModel,
                            exception = e,
                            message = ErrorMessage.Localized(
                                resource = Res.string.invalid_file_format_para
                            )
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
            send(ImportState.Idle)
            return@channelFlow
        }

        import(pack).collect {
            send(it)
        }
    }

    fun importImage(importable: Importable): String {
        val name = randomUUID() + "." + importable.name.split(".").last()
        val platform = getPlatform()
        importable.source.buffer().readAll(
            platform
                .filesystem
                .sink(platform.resourcePath.resolve(name))
        )
        return name
    }
}