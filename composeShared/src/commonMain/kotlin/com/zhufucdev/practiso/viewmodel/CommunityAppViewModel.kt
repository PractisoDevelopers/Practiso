package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import opacity.client.ArchiveMetadata
import opacity.client.SortOptions

class CommunityAppViewModel(private val server: Flow<CommunityService>) : ViewModel() {
    private val refreshCounter = MutableStateFlow(0)
    val dimensions =
        refreshCounter
            .combine(server) { _, s -> s }
            .map { server ->
                server.getDimensions(5)
            }
            .shareIn(viewModelScope, replay = 1, started = SharingStarted.Lazily)

    private val _archiveSortOptions = MutableStateFlow(SortOptions())
    private val _archivePaginator =
        refreshCounter
            .combine(_archiveSortOptions) { _, s -> s }
            .combine(server) { sort, service -> service.getArchivePagination(sort) }
            .shareIn(viewModelScope, replay = 1, started = SharingStarted.Lazily)

    @OptIn(ExperimentalCoroutinesApi::class)
    val archives =
        _archivePaginator
            .map { it.items }
            .flatMapLatest {
                it.runningFold(listOf<ArchiveMetadata>()) { acc, value -> acc + value }
            }

    var archiveSortOptions: SortOptions
        get() = _archiveSortOptions.value
        set(value) { _archiveSortOptions.value = value }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CommunityAppViewModel(
                    flowOf(CommunityService())
                )
            }
        }
    }
}