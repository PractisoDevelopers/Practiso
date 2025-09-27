package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.helper.getCommunityServiceWithDownloadManager
import com.zhufucdev.practiso.route.CommunityDimensionRouteParams
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import opacity.client.SortOptions

class CommunityDimensionViewModel(
    communityService: Flow<CommunityService>,
    downloadManager: Flow<DownloadManager>,
) : ArchiveDownloadManagedViewModel(communityService, downloadManager) {
    private val _dimension = MutableStateFlow("")
    private val _sortOptions = MutableStateFlow(SortOptions())

    private val refreshCounter = MutableStateFlow(0)
    val dimension: StateFlow<String> get() = _dimension
    val sortOptions: StateFlow<SortOptions> get() = _sortOptions

    val archives = refreshCounter
        .combine(dimension, ::Pair)
        .combine(communityService, ::Pair)
        .combine(sortOptions, ::Pair)
        .map { (pair, sortOptions) ->
            val dimension = pair.first.second
            val service = pair.second
            service.getDimensionArchivePagination(dimension)
        }
        .map { PaginatedListPresenter(it, viewModelScope) }
        .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    fun loadRouteParams(params: CommunityDimensionRouteParams) {
        _dimension.tryEmit(params.dimensionName)
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    val bundle = getCommunityServiceWithDownloadManager()
                    CommunityDimensionViewModel(
                        communityService = bundle.map { it.first },
                        downloadManager = bundle.map { it.second }
                    )
                }
            }
    }
}