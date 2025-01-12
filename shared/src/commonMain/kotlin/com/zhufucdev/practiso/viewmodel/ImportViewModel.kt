package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.ImportState
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.datamodel.importTo
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.unarchive
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okio.buffer
import okio.gzip
import okio.use
import resources.Res
import resources.failed_to_copy_resource_x_for_quiz_y_para
import resources.invalid_file_format_para
import resources.resource_x_for_quiz_y_was_not_found_para

class ImportViewModel(private val db: AppDatabase) : ViewModel() {

    val state = MutableStateFlow<ImportState>(ImportState.Idle)

    suspend fun import(it: Importable) {
        state.emit(ImportState.Unarchiving(it.name))
        val cancelChannel = Channel<Unit>()
        val pack = withContext(Dispatchers.IO) {
            try {
                it.source.gzip().buffer().unarchive()
            } catch (e: Exception) {
                state.emit(
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
            state.emit(ImportState.Idle)
            return
        }

        Channel<Unit>().let { continueChannel ->
            state.emit(
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
                state.emit(ImportState.Idle)
                return
            }
        }

        withContext(Dispatchers.IO) {
            for (index in pack.archives.quizzes.indices) {
                val quizArchive = pack.archives.quizzes[index]
                state.emit(
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
                            state.emit(
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
                                state.emit(
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

        state.emit(ImportState.Idle)
    }

    data class Events(
        val import: Channel<Importable> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.import.onReceive(::import)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ImportViewModel(Database.app)
            }
        }
    }
}