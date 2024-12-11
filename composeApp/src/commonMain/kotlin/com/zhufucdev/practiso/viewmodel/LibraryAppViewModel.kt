package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.importTo
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.unarchive
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.source
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okio.buffer
import okio.gzip
import okio.use
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.failed_to_copy_resource_x_for_quiz_y_para
import practiso.composeapp.generated.resources.invalid_file_format_para
import practiso.composeapp.generated.resources.resource_x_for_quiz_y_was_not_found_para

class LibraryAppViewModel(private val db: AppDatabase) : ViewModel() {
    val templates by lazy {
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    val quiz: Flow<List<PractisoOption.Quiz>> by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
    }

    val dimensions by lazy {
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.quizQueries)
    }

    data class Events(
        val removeQuiz: Channel<Long> = Channel(),
        val import: Channel<PlatformFile> = Channel(),
    )

    val event = Events()

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

    val importState = MutableStateFlow<ImportState>(ImportState.Idle)

    private suspend fun removeQuizWithResources(id: Long) {
        val quizFrames = db.quizQueries
            .getQuizFrames(db.quizQueries.getQuizById(id))
            .firstOrNull()
        if (quizFrames.isNullOrEmpty()) {
            return
        }

        withContext(Dispatchers.IO) {
            val platform = getPlatform()
            quizFrames.first()
                .frames
                .map(PrioritizedFrame::frame)
                .resources()
                .map { (name) ->
                    async {
                        platform.filesystem.delete(
                            platform.resourcePath.resolve(
                                name
                            )
                        )
                    }
                }
                .awaitAll()
        }

        db.transaction {
            db.quizQueries.removeQuiz(id)
        }
    }

    suspend fun import(it: PlatformFile) {
        importState.emit(ImportState.Unarchiving(it.name))
        val cancelChannel = Channel<Unit>()
        val pack = withContext(Dispatchers.IO) {
            try {
                it.source().gzip().buffer().unarchive()
            } catch (e: Exception) {
                importState.emit(
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
            importState.emit(ImportState.Idle)
            return
        }

        Channel<Unit>().let { continueChannel ->
            importState.emit(
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
                importState.emit(ImportState.Idle)
                return
            }
        }

        for (index in pack.archives.quizzes.indices) {
            val quizArchive = pack.archives.quizzes[index]
            importState.emit(
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
                        importState.emit(
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
                            importState.emit(
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

        importState.emit(ImportState.Idle)
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.removeQuiz.onReceive {
                        removeQuizWithResources(it)
                    }

                    event.import.onReceive {
                        import(it)
                    }
                }
            }
        }
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    LibraryAppViewModel(Database.app)
                }
            }
    }
}