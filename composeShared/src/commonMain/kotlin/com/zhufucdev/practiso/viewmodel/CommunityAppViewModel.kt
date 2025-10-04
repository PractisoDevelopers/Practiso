package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.DownloadException
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.getCommunityServiceWithDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import opacity.client.SortOptions

class CommunityAppViewModel(
    communityService: Flow<CommunityService>,
    downloadManager: Flow<DownloadManager>,
) : ArchiveDownloadManagedViewModel(communityService, downloadManager) {
    private val refreshCounter = MutableStateFlow(0)
    private val _pageState = MutableStateFlow<PageState>(Loading)

    val event = Events()

    init {
        viewModelScope.launch {
            while (isActive) {
                select {
                    event.refresh.onReceive {
                        _pageState.tryEmit(Loading)
                        refreshCounter.getAndUpdate { it + 1 }
                    }

                    event.mountNextPage.onReceive {
                        viewModelScope.launch(Dispatchers.IO) {
                            archivePresenter.first()?.mountNextPage()
                        }
                    }
                }
            }
        }
    }

    val pageState: StateFlow<PageState> get() = _pageState

    val dimensions =
        refreshCounter
            .combine(communityService, ::Pair)
            .map { (_, server) -> server.getDimensions(5) }
            .catch { markPageFailed(it) }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    private val _archiveSortOptions = MutableStateFlow(SortOptions())

    @OptIn(ExperimentalCoroutinesApi::class)
    val archivePresenter =
        refreshCounter
            .combine(_archiveSortOptions) { _, s -> s }
            .combine(communityService) { sort, service -> service.getArchivePagination(sort) }
            .shareIn(viewModelScope, replay = 1, started = SharingStarted.Lazily)
            .map { PaginatedListPresenter(it, viewModelScope) }
            .onEach {
                it.appendErrorHandler { err -> markPageFailed(err); true }
            }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    var archiveSortOptions: SortOptions
        get() = _archiveSortOptions.value
        set(value) {
            _archiveSortOptions.value = value
        }

    private fun markPageFailed(reason: Throwable) {
        _pageState.tryEmit(
            Failed(
                if (reason is AppException) {
                    reason as Exception
                } else {
                    DownloadException(
                        reason,
                        scope = AppScope.CommunityService,
                        appMessage = AppMessage.GenericFailure
                    )
                }
            )
        )
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        val Factory = viewModelFactory {
            val bundle = getCommunityServiceWithDownloadManager()
            initializer {
                CommunityAppViewModel(
                    bundle.map { it.first },
                    bundle.map { it.second }
                )
            }
        }
    }

    sealed class PageState
    data object Loading : PageState()
    data object Loaded : PageState()
    data class Failed(val reason: Exception) : PageState()

    data class Events(
        val mountNextPage: Channel<Unit> = Channel(),
        val refresh: Channel<Unit> = Channel(),
    )
}