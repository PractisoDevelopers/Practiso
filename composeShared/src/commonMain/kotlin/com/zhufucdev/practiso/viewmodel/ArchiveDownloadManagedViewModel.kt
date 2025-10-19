package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.UniqueContext
import com.zhufucdev.practiso.UniqueIO
import com.zhufucdev.practiso.datamodel.downloadTaskId
import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadStopped
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.ImportState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import opacity.client.ArchiveMetadata

abstract class ArchiveDownloadManagedViewModel(
    private val communityService: Flow<CommunityService>,
    private val downloadManager: Flow<DownloadManager>,
) : ViewModel() {
    data class Events(
        val downloadAndImport: Channel<Pair<ArchiveMetadata, ImportViewModel.Events>> = Channel(),
        val cancelDownload: Channel<ArchiveMetadata> = Channel(),
    )

    private val _downloadError = Channel<Exception>()

    val downloadError: Flow<Exception?> get() = _downloadError.receiveAsFlow()
    val archiveEvent = Events()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDownloadStateFlow(archive: ArchiveMetadata): StateFlow<DownloadCycle> =
        downloadManager.flatMapMerge {
            it[archive.downloadTaskId]
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = DownloadStopped.Idle
        )

    init {
        viewModelScope.launch {
            while (isActive) {
                select {
                    archiveEvent.downloadAndImport.onReceive { (archive, e) ->
                        viewModelScope.launch(Dispatchers.UniqueIO + UniqueContext(sessionId = archive.id)) {
                            downloadAndImport(archive, e)
                        }
                    }

                    archiveEvent.cancelDownload.onReceive { archive ->
                        downloadManager.first()
                            .cancel(with(communityService.first()) { archive.toHandle().taskId })
                    }
                }
            }
        }
    }

    private suspend fun downloadAndImport(archive: ArchiveMetadata, e: ImportViewModel.Events) {
        val downloadManager = downloadManager.first()
        val handle = with(communityService.first()) {
            archive.toHandle()
        }
        val pack = try {
            handle.getAsSource(downloadManager)
        } catch (_: CancellationException) {
            // noop
            return
        } catch (e: Exception) {
            _downloadError.send(e)
            return
        }
        e.import.trySend(pack)

        // remove cache afterwards
        if (e.importFinish.first() == ImportState.IdleReason.Completion) {
            (downloadManager[handle.taskId].value.takeIf { it is DownloadState.Completed } as DownloadState.Completed?)
                ?.destination
                ?.let {
                    getPlatform().filesystem.delete(it)
                }
            downloadManager.cancel(handle.taskId)
        }
    }
}