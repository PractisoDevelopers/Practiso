package com.zhufucdev.practiso.datamodel

/**
 * Used in [EmbeddingOutput] and [com.zhufucdev.practiso.service.RecommendationService]
 * to scale the output to the range of [0, 1], representing the chance of recommendation.
 */
typealias Normalizer = (Float) -> Float

data object CosineNormalizer : Normalizer by { if (it > 0) 1f - it else 0f }
