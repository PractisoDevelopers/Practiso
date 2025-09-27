package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.composable.FilterController
import com.zhufucdev.practiso.composable.SomeGroup
import com.zhufucdev.practiso.helper.getCommunityServiceWithDownloadManager
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import opacity.client.ArchiveMetadata
import opacity.client.ArchivePreview

@OptIn(SavedStateHandleSaveableApi::class)
class CommunityArchiveViewModel(
    state: SavedStateHandle,
    communityService: Flow<CommunityService>,
    downloadManager: Flow<DownloadManager>,
) : ArchiveDownloadManagedViewModel(communityService, downloadManager) {
    private val _archive by state.saveable(saver = protobufMutableStateFlowSaver<ArchiveMetadata?>()) {
        MutableStateFlow(null)
    }
    val archive: StateFlow<ArchiveMetadata?> get() = _archive

    private val refreshCounter = MutableStateFlow(0)
    private val routeParams = MutableStateFlow<ArchivePreviewRouteParams?>(null)
    val preview =
        refreshCounter
            .combine(archive) { c, archive -> c to archive?.id }
            .combine(communityService, ::Pair)
            .map { (pair, service) ->
                val (_, archiveId) = pair
                archiveId?.let { service.getArchivePreview(it) }
            }
            .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)
    val filterController =
        preview
            .combine(routeParams) { p, r -> p to r?.selectedDimensions }
            .map { (preview, selection) ->
                FilterController(preview ?: emptyList(), ArchivePreview::dimensions).apply {
                    if (selection == null) {
                        groupedItems.keys.forEach { toggleGroup(it, selected = true) }
                    } else {
                        selection
                            .map { SomeGroup(it) }
                            .forEach { toggleGroup(it, selected = true) }
                    }
                }
            }
            .stateIn(
                viewModelScope, started = SharingStarted.Lazily,
                initialValue = FilterController(emptyList(), ArchivePreview::dimensions)
            )
    val dimojis =
        archive
            .map { it?.dimensions }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    fun loadParameters(routeParams: ArchivePreviewRouteParams) {
        _archive.tryEmit(routeParams.metadata)
        this.routeParams.tryEmit(routeParams)
    }

    companion object Companion {
        val Factory = viewModelFactory {
            val bundle = getCommunityServiceWithDownloadManager()
            initializer {
                CommunityArchiveViewModel(
                    createPlatformSavedStateHandle(),
                    communityService = bundle.map { it.first },
                    downloadManager = bundle.map { it.second })
            }
        }
    }
}