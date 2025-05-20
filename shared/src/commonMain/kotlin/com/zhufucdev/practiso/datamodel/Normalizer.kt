package com.zhufucdev.practiso.datamodel

import kotlin.math.pow

/**
 * Used in [EmbeddingOutput] and [com.zhufucdev.practiso.service.RecommendationService]
 * to scale the output to the range of [0, 1], representing the chance of recommendation.
 */
typealias Normalizer = (Float) -> Float

data object CosineDistanceNormalizer : Normalizer by { maxOf(1 - it, 0f) }

data class CosinePowerNormalizer(val n: Int = 2) : Normalizer by { maxOf(1 - it.pow(n), 0f) }