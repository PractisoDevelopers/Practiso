package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.DEFAULT_COMMUNITY_SERVER_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
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
                it.runningReduce { acc, value -> acc + value }
            }

    var archiveSortOptions: SortOptions
        get() = _archiveSortOptions.value
        set(value) {
            _archiveSortOptions.value = value
        }

    private val _isMountingNextPage = MutableStateFlow(false)
    val isMountingNextPage: StateFlow<Boolean> = _isMountingNextPage

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasNextPage = _archivePaginator.flatMapLatest { it.hasNext }

    fun mountNextPage() {
        _isMountingNextPage.tryEmit(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _archivePaginator.first().mountNext()
            } catch (e: Exception) {
                // TODO: handle [e]
            } finally {
                _isMountingNextPage.tryEmit(false)
            }
        }
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        val Factory = viewModelFactory {
            initializer {
                CommunityAppViewModel(
                    AppSettings.communityServerUrl.combine(
                        AppSettings.communityUseCustomServer,
                        ::Pair
                    ).mapLatest { (server, use) ->
                        CommunityService(server.takeIf { use } ?: DEFAULT_COMMUNITY_SERVER_URL)
                    }
                )
            }
        }
    }
}