package com.zhufucdev.practiso

import com.zhufucdev.practiso.platform.DownloadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A special dispatcher that does not dispatch duplicated download requests.
 * Eventually, dispatch is handled by [Dispatchers.IO].
 */
object DownloadDispatcher : CoroutineDispatcher() {
    private val _tasks = mutableMapOf<String, DownloadContext>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (context is DownloadContext && get(context.taskId).value !is DownloadState.Completed) {
            _tasks[context.taskId] = context
        }
        Dispatchers.IO.dispatch(context, block)
    }

    operator fun get(taskId: String): StateFlow<DownloadState?> =
        _tasks.getOrPut(taskId) { DownloadContext(taskId) }.state

    operator fun set(taskId: String, state: DownloadState) {
        (get(taskId) as MutableStateFlow).tryEmit(state)
    }
}

val Dispatchers.Download: DownloadDispatcher get() = DownloadDispatcher

data class DownloadContext(val taskId: String) : CoroutineContext.Element {
    override val key = Key
    val state = MutableStateFlow<DownloadState?>(null)

    object Key : CoroutineContext.Key<DownloadContext>
}
