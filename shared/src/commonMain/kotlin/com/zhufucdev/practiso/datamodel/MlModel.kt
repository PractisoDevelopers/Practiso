package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.platform.Language
import usearch.MetricKind
import usearch.ScalarKind

sealed class ModelFeature

data class LanguageInput(val supports: Set<Language>) : ModelFeature() {
    companion object {
        fun of(vararg language: Language): LanguageInput {
            return LanguageInput(language.toSet())
        }
    }
}

data object ImageInput : ModelFeature()

data class EmbeddingOutput(
    val metric: MetricKind,
    val dimensions: ULong,
    val precision: ScalarKind,
    val normalizer: Normalizer,
) : ModelFeature()

data class TokenInput(val sequenceLength: Int) : ModelFeature()

data object AnyEmbeddingOutput : ModelFeature()

open class MlModel(val hfId: String, val features: Set<ModelFeature>) {
    val inputLanguages: Set<Language>
        get() = features.filterIsInstance<LanguageInput>()
            .fold(mutableSetOf()) { accu, curr ->
                accu.addAll(curr.supports)
                accu
            }

    val sequenceLength: Int?
        get() = features.filterFirstIsInstanceOrNull<TokenInput>()?.sequenceLength

    val embeddingOutput: EmbeddingOutput?
        get() = features.filterFirstIsInstanceOrNull<EmbeddingOutput>()
}
