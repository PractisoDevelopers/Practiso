package com.zhufucdev.practiso.helper

import usearch.Index
import usearch.IndexOptions
import usearch.MetricKind
import usearch.ScalarKind


fun calculateCosSimilarity(v1: FloatArray, v2: FloatArray): Float {
    if (v1.size != v2.size) {
        error("Expected v1.size (${v1.size}) == v2.size(${v2.size})")
    }
    val index = Index(
        IndexOptions(
            dimensions = v1.size.toULong(),
            metric = MetricKind.Cos,
            quantization = ScalarKind.F32
        )
    )
    index.asF32.add(0u, v1)
    index.asF32.add(1u, v2)
    return index.search(v1, 10).first { it.key.toInt() == 1 }.distance
}
