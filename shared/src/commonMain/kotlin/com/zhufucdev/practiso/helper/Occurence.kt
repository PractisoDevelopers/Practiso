package com.zhufucdev.practiso.helper

fun <T> List<T>.occurrence(filter: (T) -> Boolean): Map<T, Int> = buildMap {
    this@occurrence.forEach {
        put(it, 0)
    }

    this@occurrence.forEach {
        if (filter(it)) {
            put(it, this@buildMap[it]!! + 1)
        }
    }
}

fun <T> List<T>.ratioOf(filter: (T) -> Boolean): Map<T, Float> {
    val frequency = buildMap {
        this@ratioOf.forEach {
            put(it, (get(it) ?: 0) + 1)
        }
    }

    return occurrence(filter).mapValues { (key, value) -> value.toFloat() / frequency[key]!! }
}