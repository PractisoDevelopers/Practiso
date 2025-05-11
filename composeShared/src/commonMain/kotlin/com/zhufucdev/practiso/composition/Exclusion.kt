package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ExclusionLock(val current: MutableStateFlow<Int?> = MutableStateFlow(null))

private var id = 0
private val idMutex = Mutex()

@Composable
fun withExclusionLock(lock: ExclusionLock, composable: @Composable ExclusionLockScope.() -> Unit) {
    val localId by produceState(-1) {
        idMutex.withLock {
            value = id++
        }
    }
    val currentLockId by lock.current.collectAsState()

    composable(object : ExclusionLockScope {
        override val localId: Int
            get() = localId

        override suspend fun lock() {
            lock.current.emit(localId)
        }

        override suspend fun release() {
            if (localId == currentLockId) {
                lock.current.emit(null)
            }
        }

        @Composable
        override fun LaunchOstracization(block: suspend () -> Unit) {
            LaunchedEffect(currentLockId) {
                if (currentLockId != localId) {
                    block()
                }
            }
        }
    })
}

interface ExclusionLockScope {
    val localId: Int

    @Composable
    fun LaunchOstracization(block: suspend () -> Unit)
    suspend fun lock()
    suspend fun release()
}