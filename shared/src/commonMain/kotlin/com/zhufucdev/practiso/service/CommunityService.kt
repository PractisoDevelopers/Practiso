package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opacity.client.ArchiveMetadata
import opacity.client.DimensionMetadata
import opacity.client.OpacityClient
import opacity.client.SortOptions

class CommunityService(endpoint: String = "https://opacity.zhufucdev.com") {
    private val client = OpacityClient(endpoint, PlatformHttpClientFactory)

    fun getArchivePagination(sortOptions: SortOptions = SortOptions()) =
        object : Paginated<ArchiveMetadata> {
            private val pageRequestChannel = Channel<Unit>()
            private val pageCompleteChannel = Channel<Unit>()

            override val items: Flow<List<ArchiveMetadata>> = flow {
                var response = client.getArchiveList(sortOptions)
                emit(response.page)
                while (response.next != null) {
                    pageRequestChannel.receive()

                    response = client.getArchiveList(sortOptions, response.next)
                    emit(response.page)
                }
                pageCompleteChannel.close(IllegalStateException("Already on the last page."))
            }

            override suspend fun mountNext() {
                pageRequestChannel.send(Unit)
                pageCompleteChannel.receive()
            }
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
}
