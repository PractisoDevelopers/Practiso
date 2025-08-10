package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.datamodel.ArchiveHandle
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import opacity.client.DimensionMetadata
import opacity.client.OpacityClient
import opacity.client.SortOptions

class CommunityService(
    endpoint: String = DEFAULT_COMMUNITY_SERVER_URL,
) {
    private val client = OpacityClient(endpoint, PlatformHttpClientFactory)

    fun getArchivePagination(sortOptions: SortOptions = SortOptions()) =
        object : Paginated<ArchiveHandle> {
            private val pageRequestChannel = Channel<Unit>()
            private val pageCompleteChannel = Channel<Unit>()

            override val items: Flow<List<ArchiveHandle>> = flow {
                var response = client.getArchiveList(sortOptions)
                emit(response.page.map { ArchiveHandle(it, client) })
                while (response.next != null) {
                    pageRequestChannel.receive()

                    response = client.getArchiveList(sortOptions, response.next)
                    emit(response.page.map { ArchiveHandle(it, client) })
                    pageCompleteChannel.send(Unit)
                }
                hasNext.tryEmit(false)
                pageRequestChannel.close(LastPageException)
                pageCompleteChannel.close(LastPageException)
            }

            override suspend fun mountNext() {
                pageRequestChannel.send(Unit)
                pageCompleteChannel.receive()
            }

            override val hasNext = MutableStateFlow(true)
        }

    suspend fun getDimensions(takeFirst: Int = 20): List<DimensionMetadata> =
        client.getDimensionList(takeFirst)
}

/**
 * List of [T] items with paging.
 */
interface Paginated<T> {
    /**
     * The currently mounted [Flow].
     * Shall not emit empty lists unless the data source is empty.
     */
    val items: Flow<List<T>>

    /**
     * Load the next page and update [items].
     * Shall be suspended until the next page is fully mounted.
     */
    suspend fun mountNext()

    val hasNext: StateFlow<Boolean>
}

object LastPageException : IllegalStateException("Already on the last page.")

const val DEFAULT_COMMUNITY_SERVER_URL = "https://opacity.zhufucdev.com"