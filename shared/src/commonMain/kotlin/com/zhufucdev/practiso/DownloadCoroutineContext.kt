package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.DownloadException
import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadEnd
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.randomUUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

private val states = mutableMapOf<String, MutableStateFlow<DownloadCycle>>()
private val sessions = mutableMapOf<String, String>()
private val rwMutex = Mutex()

/**
 * A special dispatcher that does not dispatch duplicated download requests.
 * Eventually, dispatch is handled by [Dispatchers.IO].
 */
object DownloadDispatcher : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return context[DownloadContext.Key] != null
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        val ctx = context[DownloadContext.Key]!!
        val differentSession =
            sessions[ctx.taskId]?.let { it != ctx.sessionId } == true
        val state = states[ctx.taskId]?.value
        val notCompleted =
            state?.let { it !is DownloadState.Completed } == true

        if (differentSession && notCompleted) {
            return
        }
        if (differentSession && (!notCompleted || state is DownloadEnd)) {
            sessions[ctx.taskId] = ctx.sessionId
        }

        Dispatchers.IO.dispatch(context, block)
    }

    suspend operator fun get(taskId: String): StateFlow<DownloadCycle> = rwMutex.withLock {
        states.getOrPut(taskId) { MutableStateFlow(DownloadEnd.Idle) }
    }

    suspend operator fun set(taskId: String, state: DownloadState) {
        (get(taskId) as MutableStateFlow).emit(state)
    }

    suspend fun remove(taskId: String) = rwMutex.withLock {
        states.remove(taskId)
    }
}

val Dispatchers.Download: CoroutineContext
    get() = DownloadDispatcher + CoroutineExceptionHandler { context, e ->
        val downloadCtx = context[DownloadContext.Key]
        if (downloadCtx != null) {
            states[downloadCtx.taskId]?.tryEmit(
                DownloadEnd.Error(
                    e as? Exception
                        ?: DownloadException(
                            cause = e,
                            scope = AppScope.DownloadExecutor,
                            appMessage = AppMessage.GenericFailure
                        )
                )
            )
            sessions.remove(downloadCtx.sessionId)
        }
        e.printStackTrace()
    }

data class DownloadContext(val taskId: String, val sessionId: String = randomUUID()) :
    CoroutineContext.Element {
    override val key = Key

    object Key : CoroutineContext.Key<DownloadContext>
}
