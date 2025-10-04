package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.ExportService
import com.zhufucdev.practiso.service.UploadArchive
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.okio.asOkioSink
import okio.buffer
import okio.gzip
import opacity.client.BonjourResponse
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.empty_span
import resources.new_question_para
import resources.new_question_span
import resources.x_and_n_more_para
import resources.x_and_y_span

expect class ArchiveSharingViewModel : CommonArchiveSharingViewModel {
    companion object Companion {
        val Factory: ViewModelProvider.Factory
    }
}

abstract class CommonArchiveSharingViewModel(
    private val db: AppDatabase,
    exportService: ExportService,
    val communityService: Flow<CommunityService>,
) : ViewModel() {
    private val _exportToFile = Channel<PlatformFile>()
    private val _uploadToCommunity = Channel<Unit>()

    val exportToFile: SendChannel<PlatformFile> = _exportToFile
    val uploadToCommunity: SendChannel<Unit> = _uploadToCommunity

    var selection: Collection<Long> by mutableStateOf(emptySet())
        private set
    var uploadState: UploadArchive? by mutableStateOf(null)

    val serverInfo: StateFlow<BonjourResponse?> =
        communityService.map { it.getServerInfo() }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    fun loadParameters(selection: Collection<Long>) {
        this.selection = selection
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                select {
                    _exportToFile.onReceive { destFile ->
                        destFile.sink().asOkioSink().gzip()
                            .use { sink ->
                                exportService.exportAsSource(selection)
                                    .buffer()
                                    .use { source ->
                                        source.readAll(sink)
                                    }
                            }
                    }

                    _uploadToCommunity.onReceive {
                        val community = communityService.first()
                        val platform = getPlatform()
                        val temporaryFilePath =
                            platform.createTemporaryFile(
                                prefix = "share-with-community",
                                suffix = ".psarchive"
                            )
                        withContext(Dispatchers.IO) {
                            val temporarySink = platform.filesystem.sink(temporaryFilePath)
                            temporarySink.gzip().buffer().use { sink ->
                                exportService.exportAsSource(selection)
                                    .buffer()
                                    .use { source -> source.readAll(sink) }
                            }
                        }
                        community.uploadArchive(
                            SystemFileSystem.source(kotlinx.io.files.Path(temporaryFilePath.toString()))
                                .buffered()
                        )
                            .collect { state ->
                                uploadState = state
                            }
                    }
                }
            }
        }
    }

    suspend fun describeSelection(): String {
        val quizzes = db.quizQueries.getQuizByIds(selection)
            .executeAsList()
            .sortedBy { it.name }

        if (quizzes.isEmpty()) {
            return getString(Res.string.empty_span)
        }

        val nMore = quizzes.size - 2
        return if (nMore >= 1) {
            val names = coroutineScope {
                quizzes.slice(0 until 2).mapIndexed { index, quiz ->
                    async {
                        quiz.name
                            ?: getString(
                                if (index == 0) Res.string.new_question_para
                                else Res.string.new_question_span
                            )
                    }
                }
            }.awaitAll().joinToString()
            getString(Res.string.x_and_n_more_para, names, nMore)
        } else if (nMore == 0) {
            getString(
                Res.string.x_and_y_span,
                quizzes[0].name ?: getString(Res.string.new_question_para),
                quizzes[1].name ?: getString(Res.string.new_question_span)
            )
        } else {
            quizzes.first().name ?: getString(Res.string.new_question_para)
        }
    }
}