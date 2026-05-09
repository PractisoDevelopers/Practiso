package com.zhufucdev.practiso.helper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

fun <T> Flow<List<T>>.concat(others: Flow<List<T>>) = combine(others) { a, b -> a + b }

fun <T> Flow<T>.mapToResults() = flow {
    this@mapToResults.catch { emit(Result.failure(it)) }.collect {
        emit(Result.success(it))
    }
}