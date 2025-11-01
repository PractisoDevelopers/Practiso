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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive

/**
 * Presents [Paginated] in a continuous, infinite-scrolling list.
 */
@Stable
class PaginatedListPresenter<T>(
    private val inner: Paginated<T>,
    lifecycleScope: CoroutineScope,
    val preloadExtent: Int = 3,
    listDelegate: MutableList<T> = mutableStateListOf()
) {
    var hasNextPage by mutableStateOf(inner.hasNext)
        private set
    var isMounting by mutableStateOf(true)
        private set
    private val itemCollectorSupervisor: Deferred<Unit>
    private val _items = listDelegate
    val items: List<T> get() = _items

    init {
        itemCollectorSupervisor = lifecycleScope.async {
            inner.items.collect {
                _items.addAll(it)
                isMounting = false
            }
        }
        itemCollectorSupervisor.invokeOnCompletion { err ->
            if (err != null) {
                handle(err)
            }
        }
    }

    /**
     * Mount the next page and update [listDelegate].
     * @return Any exception associated with this mount.
     * Can be null if it is handled by [PaginatedListPresenterErrorHandler]
     * registered by using [appendErrorHandler].
     */
    suspend fun mountNextPage(): Exception? {
        isMounting = true
        try {
            itemCollectorSupervisor.apply {
                if (!isCompleted || isCancelled) {
                    ensureActive()
                }
            }
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

    private fun handle(error: Throwable): Boolean {
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
typealias PaginatedListPresenterErrorHandler<T> = PaginatedListPresenter<T>.(Throwable) -> Boolean
