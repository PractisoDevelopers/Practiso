package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.AnyEmbeddingOutput
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.ImageInput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.ModelFeature
import com.zhufucdev.practiso.datamodel.TokenInput
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.embedding_output_span
import resources.image_input_span
import resources.language_input_x_span
import resources.token_input_span

@Composable
fun modelFeatureString(feature: ModelFeature): String =
    when (feature) {
        is EmbeddingOutput, AnyEmbeddingOutput -> stringResource(Res.string.embedding_output_span)
        ImageInput -> stringResource(Res.string.image_input_span)
        is LanguageInput -> stringResource(
            Res.string.language_input_x_span,
            feature.supports.map { it.name }.sorted().joinToString()
        )

        is TokenInput -> stringResource(Res.string.token_input_span) + "(${feature.sequenceLength})"
    }
