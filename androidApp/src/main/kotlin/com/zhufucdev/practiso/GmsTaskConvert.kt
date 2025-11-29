package com.zhufucdev.practiso

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener {
        continuation.resume(it.result)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
    addOnFailureListener {
        continuation.resumeWithException(it)
    }
}