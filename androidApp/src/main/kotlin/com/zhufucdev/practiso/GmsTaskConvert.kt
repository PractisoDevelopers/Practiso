package com.zhufucdev.practiso

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@OptIn(InternalCoroutinesApi::class)
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener {
        val token = continuation.tryResume(it.result)
        if (token != null) {
            continuation.completeResume(token)
        }
    }
    addOnCanceledListener {
        continuation.cancel()
    }
    addOnFailureListener {
        continuation.resumeWithException(it)
    }
}