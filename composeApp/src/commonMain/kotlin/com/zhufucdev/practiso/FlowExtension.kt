package com.zhufucdev.practiso

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

fun <T> Flow<List<T>>.concat(others: Flow<List<T>>) = combine(others) { a, b -> a + b }