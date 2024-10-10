package com.zhufucdev.practiso

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onCompletion

fun <T> Flow<T>.concatOrThrow(others: Flow<T>) = onCompletion {
    if (it == null) {
        emitAll(others)
    } else {
        throw it
    }
}