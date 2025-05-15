package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.SnackbarExtension
import com.zhufucdev.practiso.datamodel.AnyEmbeddingOutput
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.ImageInput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.ModelFeature
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.service.FeiDbState
import com.zhufucdev.practiso.service.MissingModelResponse
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_cube_off
import resources.cancel_para
import resources.collecting_questions_para
import resources.current_model_is_missing_following_features
import resources.details_para
import resources.embedding_output_span
import resources.image_input_span
import resources.inferring_n_items_para
import resources.language_input_x_span
import resources.missing_model_para
import resources.proceed_anyway_para

@Composable
fun FeiStatus(state: FeiDbState) {
    val snackbar = LocalExtensiveSnackbarState.current
    var detailsDialog by remember { mutableStateOf<MissingModelDialog>(MissingModelDialog.Hidden) }

    LaunchedEffect(state) {
        when (state) {
            FeiDbState.Collecting -> snackbar.showSnackbar(
                getString(Res.string.collecting_questions_para),
                SnackbarExtension.Identifier(FeiStatusBarDefaultId),
                duration = SnackbarDuration.Indefinite
            )

            is FeiDbState.InProgress -> {
                if (snackbar.extensions.any { it is SnackbarExtension.Identifier<*> && it.id is FeiStatusBarDefaultId }) {
                    val emitter = (snackbar.extensions.filterFirstIsInstanceOrNull<SnackbarExtension.ProgressBar>()
                            ?.progress as? MutableStateFlow<Float>)
                    if (emitter != null) {
                        emitter.emit(state.done.toFloat() / state.total)
                        return@LaunchedEffect
                    }
                }
                snackbar.showSnackbar(
                    getPluralString(
                        Res.plurals.inferring_n_items_para,
                        state.total,
                        state.total
                    ),
                    SnackbarExtension.Identifier(FeiStatusBarDefaultId),
                    SnackbarExtension.ProgressBar(MutableStateFlow(state.done.toFloat() / state.total)),
                    duration = SnackbarDuration.Indefinite
                )
            }

            is FeiDbState.MissingModel -> {
                val response = snackbar.showSnackbar(
                    getString(Res.string.missing_model_para),
                    SnackbarExtension.Identifier(FeiStatusBarDefaultId),
                    actionLabel = getString(Res.string.details_para),
                    withDismissAction = state.proceed == null,
                    duration = SnackbarDuration.Indefinite
                )

                when (response) {
                    SnackbarResult.Dismissed -> {
                        state.proceed?.send(MissingModelResponse.Cancel)
                    }

                    SnackbarResult.ActionPerformed -> {
                        detailsDialog = MissingModelDialog.Shown(state)
                    }
                }
            }

            is FeiDbState.Ready -> {
                if (snackbar.extensions.any { it is SnackbarExtension.Identifier<*> && it.id is FeiStatusBarDefaultId }) {
                    snackbar.host.currentSnackbarData?.dismiss()
                }
            }
        }
    }

    when (val dialog = detailsDialog) {
        is MissingModelDialog.Shown -> {
            val proceed = dialog.data.proceed

            AlertDialog(
                onDismissRequest = {
                    detailsDialog = MissingModelDialog.Hidden
                },
                confirmButton = {
                    if (proceed != null) {
                        TextButton(
                            onClick = {
                                proceed.trySend(MissingModelResponse.ProceedAnyway)
                                detailsDialog = MissingModelDialog.Hidden
                            }
                        ) {
                            Text(stringResource(Res.string.proceed_anyway_para))
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        proceed?.trySend(MissingModelResponse.Cancel)
                        detailsDialog = MissingModelDialog.Hidden
                    }) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                },
                icon = {
                    Icon(
                        painterResource(Res.drawable.baseline_cube_off),
                        contentDescription = null
                    )
                },
                title = {
                    Text(stringResource(Res.string.missing_model_para))
                },
                text = {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                pluralStringResource(
                                    Res.plurals.current_model_is_missing_following_features,
                                    dialog.data.missingFeatures.size,
                                ),
                            )

                            @Suppress("SimplifiableCallChain") // here it's not possible to merge map and joinToString
                            Text(
                                dialog.data.missingFeatures.sortedBy { it::class.simpleName }
                                    .map { modelFeatureString(it) }
                                    .joinToString("\n")
                            )
                        }
                    }
                }
            )
        }

        else -> {}
    }
}

data object FeiStatusBarDefaultId

private sealed class MissingModelDialog {
    data object Hidden : MissingModelDialog()
    data class Shown(val data: FeiDbState.MissingModel) : MissingModelDialog()
}

@Composable
fun modelFeatureString(feature: ModelFeature): String =
    when (feature) {
        is EmbeddingOutput, AnyEmbeddingOutput -> stringResource(Res.string.embedding_output_span)
        ImageInput -> stringResource(Res.string.image_input_span)
        is LanguageInput -> stringResource(
            Res.string.language_input_x_span,
            feature.supports.map { it.name }.sorted().joinToString()
        )
    }
