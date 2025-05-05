package com.zhufucdev.practiso.datamodel

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

data class EmbeddingOutput(val metric: MetricKind, val dimensions: ULong, val precision: ScalarKind) : ModelFeature()

data object AnyEmbeddingOutput : ModelFeature()

open class MlModel(val hfId: String, val features: Set<ModelFeature>) {
    val inputLanguages: Set<Language> by lazy {
        features.filterIsInstance<LanguageInput>()
            .fold(mutableSetOf()) { accu, curr ->
                accu.addAll(curr.supports)
                accu
            }
    }
}
