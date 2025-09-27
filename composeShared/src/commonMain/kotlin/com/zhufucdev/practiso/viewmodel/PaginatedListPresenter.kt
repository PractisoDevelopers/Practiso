package com.zhufucdev.practiso.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zhufucdev.practiso.datamodel.LastPageException
import com.zhufucdev.practiso.datamodel.Paginated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Presents [Paginated] in a continuous, infinite-scrolling list.
 */
@Stable
class PaginatedListPresenter<T>(
    private val inner: Paginated<T>,
    lifecycleScope: CoroutineScope,
    val preloadExtent: Int = 3,
) {
    var hasNextPage by mutableStateOf(inner.hasNext)
        private set
    var isMounting by mutableStateOf(true)
        private set
    private val _items = mutableStateListOf<T>()
    val items: List<T> get() = _items

    init {
        lifecycleScope.launch {
            inner.items.collect {
                _items.addAll(it)
                isMounting = false
            }
        }
    }

    /**
     * Mount the next page and update [items].
     * @return Any exception associated with this mount.
     * Can be null if it is handled by [PaginatedListPresenterErrorHandler]
     * registered by using [appendErrorHandler].
     */
    suspend fun mountNextPage(): Exception? {
        isMounting = true
        try {
            inner.mountNext()
            return null
        } catch (e: Exception) {
            if (e is LastPageException || handle(e)) {
                return null
            }
            return e
        } finally {
            isMounting = false
            hasNextPage = inner.hasNext
        }
    }

    private val errorHandlers = mutableListOf<PaginatedListPresenterErrorHandler<T>>()
    fun appendErrorHandler(handler: PaginatedListPresenterErrorHandler<T>) {
        errorHandlers.add(handler)
    }

    private fun handle(error: Exception): Boolean {
        return errorHandlers.firstOrNull { it(error) } != null
    }

    fun shouldMountNext(scrollState: LazyListState): Boolean {
        if (!hasNextPage) {
            return false
        }
        if (isMounting) {
            return false
        }
        val lastIndexIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        return items.size - lastIndexIndex <= preloadExtent
    }
}
typealias PaginatedListPresenterErrorHandler<T> = PaginatedListPresenter<T>.(Exception) -> Boolean
