package com.zhufucdev.practiso

import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadStopped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks active download jobs and updates
 * their corresponding [states]. Use [DownloadManager].[get]
 * for reception, or [cancel] to interrupt.
 */
class DownloadManager(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val states = mutableMapOf<String, MutableStateFlow<DownloadCycle>>()
    private val trackingJobs = mutableMapOf<String, Job>()
    private val rwMutex = Mutex()

    suspend fun join(taskId: String, state: Flow<DownloadState>): TrackedTask {
        if (trackingJobs.contains(taskId)) {
            return TrackedTask(trackingJobs[taskId]!!, states[taskId]!!)
        }

        val broadcast = get(taskId)
        val newJob = coroutineScope.launch(SessionContext(sessionId = taskId)) {
            try {
                state.collect {
                    broadcast.tryEmit(it)
                }
            } catch (e: Exception) {
                broadcast.tryEmit(DownloadStopped.Error(e))
                trackingJobs.remove(taskId)
                throw e
            }
        }
        trackingJobs[taskId] = newJob
        return TrackedTask(newJob, broadcast)
    }

    suspend operator fun get(taskId: String) =
        rwMutex.withLock {
            states.getOrPut(taskId) { MutableStateFlow(DownloadStopped.Idle) }
        }

    fun cancel(taskId: String, cause: CancellationException? = null) {
        trackingJobs[taskId]?.cancel(cause)
        trackingJobs.remove(taskId)
        states[taskId]?.value = DownloadStopped.Idle
    }

    data class TrackedTask(val tracker: Job, val state: StateFlow<DownloadCycle>)
}