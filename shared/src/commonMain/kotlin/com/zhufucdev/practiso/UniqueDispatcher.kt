package com.zhufucdev.practiso

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * A special dispatcher that does not dispatch duplicated requests.
 * Eventually, dispatch is handled by parent dispatcher.
 * Duplication is determined by [UniqueContext.sessionId].
 */
open class UniqueDispatcher(private val delegate: CoroutineDispatcher = Dispatchers.Default) :
    CoroutineDispatcher() {
    private val sessions = hashSetOf<String>()
    private val rwMutex = Mutex()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return context[UniqueContext.Key] != null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        val ctx = context[UniqueContext.Key]!!
        if (!sessions.contains(ctx.sessionId)) {
            val job = context[Job]
            job?.invokeOnCompletion {
                sessions.remove(ctx.sessionId)
            }
            delegate.dispatch(context, block)
        }
    }

    suspend fun remove(taskId: String) = rwMutex.withLock {
        sessions.remove(taskId)
    }
}

private object UniqueIoDispatcher : UniqueDispatcher(Dispatchers.IO)

val Dispatchers.UniqueIO: CoroutineDispatcher get() = UniqueIoDispatcher

data class UniqueContext(
    val sessionId: String,
) :
    CoroutineContext.Element {
    override val key = Key

    object Key : CoroutineContext.Key<UniqueContext>
}
