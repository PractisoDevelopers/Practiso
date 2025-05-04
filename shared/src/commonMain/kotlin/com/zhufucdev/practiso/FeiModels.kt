package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.platform.Language
import usearch.MetricKind

data object JinaV2SmallEn : MlModel(
    hfId = "jinaai/jina-embeddings-v2-small-en",
    features = setOf(
        LanguageInput.of(Language.English, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 512u)
    )
)

data object JinaV2EnZh : MlModel(
    hfId = "jinaai/jina-embeddings-v2-base-zh",
    features = setOf(
        LanguageInput.of(Language.English, Language.Chinese, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u)
    )
)

data object JinaV2EnEs : MlModel(
    hfId = "jinaai/jina-embeddings-v2-base-es",
    features = setOf(
        LanguageInput.of(Language.English, Language.Spanish, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u)
    )
)

data object JinaV2EnDe : MlModel(
    hfId = "jinaai/jina-embeddings-v2-base-de",
    features = setOf(
        LanguageInput.of(Language.English, Language.German, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u)
    )
)

object KnownModels : List<MlModel> by listOf(JinaV2SmallEn, JinaV2EnZh, JinaV2EnEs, JinaV2EnDe) {
    operator fun get(hfId: String): MlModel? = firstOrNull { it.hfId == hfId }
}