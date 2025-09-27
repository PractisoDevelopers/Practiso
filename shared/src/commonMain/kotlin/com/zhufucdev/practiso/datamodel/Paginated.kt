package com.zhufucdev.practiso.datamodel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

    val hasNext: Boolean
}

object LastPageException : IllegalStateException("Already on the last page.")

abstract class NextPointerBasedPaginated<T, K> : Paginated<T> {
    private val pageRequestChannel = Channel<Unit>(Channel.UNLIMITED)
    private val pageCompleteChannel = Channel<Unit>()

    override val items: Flow<List<T>> = flow {
        var response = getFirstPage()
        emit(response.first)
        while (response.second != null) {
            pageRequestChannel.receive()

            response = getFollowingPages(response.second!!)
            emit(response.first)
            pageCompleteChannel.send(Unit)
        }
        hasNext = false
        pageRequestChannel.close(LastPageException)
        pageCompleteChannel.close(LastPageException)
    }

    override suspend fun mountNext() {
        pageRequestChannel.send(Unit)
        pageCompleteChannel.receive()
    }

    override var hasNext = true

    abstract suspend fun getFirstPage(): Pair<List<T>, K?>
    abstract suspend fun getFollowingPages(priorPointer: K): Pair<List<T>, K?>
}
