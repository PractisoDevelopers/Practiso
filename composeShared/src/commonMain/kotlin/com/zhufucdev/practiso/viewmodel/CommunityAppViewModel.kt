package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.AppSettingsScope
import com.zhufucdev.practiso.Download
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.DownloadException
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.DEFAULT_COMMUNITY_SERVER_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import opacity.client.SortOptions

class CommunityAppViewModel(private val server: Flow<CommunityService>) : ViewModel() {
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
                }
            }
        }
    }

    val pageState: StateFlow<PageState> get() = _pageState

    val dimensions =
        refreshCounter
            .combine(server) { _, s -> s }
            .map { server -> server.getDimensions(5) }
            .catch { markPageFailed(it) }
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
            .catch { markPageFailed(it) }

    var archiveSortOptions: SortOptions
        get() = _archiveSortOptions.value
        set(value) {
            _archiveSortOptions.value = value
        }

    private val _isMountingNextPage = MutableStateFlow(false)
    val isMountingNextPage: StateFlow<Boolean> = _isMountingNextPage

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasNextPage = _archivePaginator.flatMapLatest { it.hasNext }

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
            var downloadScope: CoroutineScope? = null
            val service = AppSettings.communityServerUrl.combine(
                AppSettings.communityUseCustomServer,
                ::Pair
            )
                .mapLatest { (server, use) ->
                    downloadScope?.cancel()
                    downloadScope = CoroutineScope(Dispatchers.Download)
                    CommunityService(
                        server.takeIf { use } ?: DEFAULT_COMMUNITY_SERVER_URL,
                        downloadScope = downloadScope
                    )
                }
                .shareIn(AppSettingsScope, started = SharingStarted.Lazily, replay = 1)
            initializer {
                CommunityAppViewModel(service)
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