package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.datamodel.AccountRemovedException
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.HttpResponseException
import com.zhufucdev.practiso.helper.removeWithRollbackablity
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.getCommunityServiceWithDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import opacity.client.ArchiveMetadata
import opacity.client.AuthorizationException
import opacity.client.HttpStatusAssertionException
import opacity.client.SortOptions
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityAppViewModel(
    communityService: Flow<CommunityService>,
    downloadManager: Flow<DownloadManager>,
) : ArchiveDownloadManagedViewModel(communityService, downloadManager) {
    private val refreshCounter = MutableStateFlow(0)
    private val _pageState = MutableStateFlow<PageState>(Loaded)
    private val _removalError = Channel<Exception>()

    val event = Events()

    init {
        viewModelScope.launch {
            while (isActive) {
                select {
                    event.refresh.onReceive {
                        _pageState.tryEmit(Loading)
                        refreshCounter.getAndUpdate { it + 1 }
                        delay(2.seconds)
                        if (_pageState.value !is Failed) {
                            _pageState.tryEmit(Loaded)
                        }
                    }

                    event.mountNextPage.onReceive {
                        viewModelScope.launch(Dispatchers.IO) {
                            archivePresenter.first()?.mountNextPage()
                        }
                    }

                    event.deleteArchive.onReceive { archiveId ->
                        viewModelScope.launch(Dispatchers.IO) {
                            val archiveViewRemoval =
                                _archiveList.removeWithRollbackablity(at = _archiveList.indexOfFirst { it.id == archiveId })
                            val communityService = communityService.first()
                            try {
                                communityService.deleteArchive(archiveId)
                            } catch (e: AuthorizationException) {
                                archiveViewRemoval?.cancel()
                                val duplex = Channel<AccountRemovedException.Action>()
                                _removalError.send(AccountRemovedException(duplex, cause = e))
                                val action = duplex.receive()
                                when (action) {
                                    AccountRemovedException.ActionCancel -> {}
                                    AccountRemovedException.ActionSignOff -> {
                                        communityService.identity.clear()
                                    }
                                }
                            } catch (e: HttpStatusAssertionException) {
                                archiveViewRemoval?.cancel()
                                _removalError.send(
                                    HttpResponseException(
                                        scope = AppScope.CommunityService,
                                        status = e.statusCode,
                                    )
                                )
                            } catch (e: Exception) {
                                archiveViewRemoval?.cancel()
                                _removalError.send(e)
                            }
                        }
                    }
                }
            }
        }
    }

    val pageState: StateFlow<PageState> get() = _pageState
    val removalError: Flow<Exception?> get() = _removalError.receiveAsFlow()

    val dimensions =
        refreshCounter
            .combine(communityService, ::Pair)
            .map { (_, server) -> server.getDimensions(5) }
            .catch { markPageFailed(it) }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    private val _archiveSortOptions = MutableStateFlow(SortOptions())
    private val _archiveList = mutableStateListOf<ArchiveMetadata>()

    val archivePresenter =
        refreshCounter
            .combine(_archiveSortOptions) { _, s -> s }
            .combine(communityService) { sort, service -> service.getArchivePagination(sort) }
            .shareIn(viewModelScope, replay = 1, started = SharingStarted.Lazily)
            .onEach { _archiveList.clear() }
            .map { PaginatedListPresenter(it, viewModelScope, listDelegate = _archiveList) }
            .onEach {
                it.appendErrorHandler { err -> markPageFailed(err); true }
            }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    var archiveSortOptions: SortOptions
        get() = _archiveSortOptions.value
        set(value) {
            _archiveSortOptions.value = value
        }

    val whoami =
        refreshCounter.combine(communityService, ::Pair)
            .mapLatest { (_, community) -> runCatching { community.getWhoami() } }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    private fun markPageFailed(reason: Throwable) {
        _pageState.tryEmit(
            Failed(
                if (reason is AppException) {
                    reason as Throwable
                } else {
                    reason
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
                    bundle.map { it.second },
                )
            }
        }
    }

    sealed class PageState
    data object Loading : PageState()
    data object Loaded : PageState()
    data class Failed(val reason: Throwable) : PageState()

    data class Events(
        val mountNextPage: Channel<Unit> = Channel(),
        val refresh: Channel<Unit> = Channel(),
        val deleteArchive: Channel<String> = Channel()
    )
}