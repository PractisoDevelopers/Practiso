package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.helper.getCommunityServiceWithDownloadManager
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import opacity.client.ArchiveMetadata

@OptIn(SavedStateHandleSaveableApi::class)
class ArchivePreviewViewModel(
    state: SavedStateHandle,
    communityService: Flow<CommunityService>,
    downloadManager: Flow<DownloadManager>,
) : ArchiveDownloadManagedViewModel(communityService, downloadManager) {
    private val _archive by state.saveable(saver = protobufMutableStateFlowSaver<ArchiveMetadata?>()) {
        MutableStateFlow(null)
    }
    val archive: StateFlow<ArchiveMetadata?> get() = _archive

    private val refreshCounter = MutableStateFlow(0)
    val preview =
        refreshCounter
            .combine(archive) { c, archive -> c to archive?.id }
            .combine(communityService, ::Pair)
            .map { (pair, service) ->
                val (_, archiveId) = pair
                archiveId?.let { service.getArchivePreview(it) }
            }
    val dimojis = archive.map { it?.dimensions }

    fun loadParameters(routeParams: ArchivePreviewRouteParams) {
        _archive.tryEmit(routeParams.metadata)
    }

    companion object {
        val Factory = viewModelFactory {
            val bundle = getCommunityServiceWithDownloadManager()
            initializer {
                ArchivePreviewViewModel(
                    createPlatformSavedStateHandle(),
                    communityService = bundle.map { it.first },
                    downloadManager = bundle.map { it.second })
            }
        }
    }
}