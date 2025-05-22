package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.SnackbarExtension
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.service.FeiDbState
import com.zhufucdev.practiso.service.MissingModelResponse
import com.zhufucdev.practiso.service.PendingDownloadResponse
import com.zhufucdev.practiso.style.PaddingBig
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.about_to_download_x_items_will_cost_y_of_data_when_to_para
import resources.baseline_cloud_download
import resources.baseline_cube_off
import resources.cancel_para
import resources.collecting_questions_para
import resources.current_model_is_missing_following_features
import resources.details_para
import resources.download_required_para
import resources.downloading_model_para
import resources.inferring_n_items_para
import resources.missing_model_para
import resources.now_para
import resources.proceed_anyway_para
import resources.using_wifi_only_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeiStatus(state: FeiDbState) {
    val coroutine = rememberCoroutineScope()
    val snackbar = LocalExtensiveSnackbarState.current
    var detailsDialog: MissingModelDialog by remember { mutableStateOf(MissingModelDialog.Hidden) }
    var downloadDialog: PendingDownloadDialog by remember { mutableStateOf(PendingDownloadDialog.Hidden) }
    var snackbarInferenceProgressJob: Job? by remember { mutableStateOf(null) }
    var snackbarDownloadProgressJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(state) {
        if (state !is FeiDbState.InProgress) {
            snackbarInferenceProgressJob?.cancel()
        }
        if (state !is FeiDbState.DownloadingInference) {
            snackbarDownloadProgressJob?.cancel()
        }

        when (state) {
            FeiDbState.Collecting -> snackbar.showSnackbar(
                getString(Res.string.collecting_questions_para),
                SnackbarExtension.Identifier(FeiStatusBarDefaultId),
                duration = SnackbarDuration.Indefinite
            )

            is FeiDbState.PendingDownload -> {
                val response = snackbar.showSnackbar(
                    getString(Res.string.download_required_para),
                    actionLabel = getString(Res.string.details_para),
                    duration = SnackbarDuration.Indefinite
                )
                if (response == SnackbarResult.ActionPerformed) {
                    downloadDialog = PendingDownloadDialog.Proceed(state.files, state.response)
                }
            }

            is FeiDbState.DownloadingInference -> {
                if (snackbar.extensions.any { it is SnackbarExtension.Identifier<*> && it.id == FeiStatusBarDownloadId }) {
                    val emitter =
                        (snackbar.extensions.filterFirstIsInstanceOrNull<SnackbarExtension.ProgressBar>()
                            ?.progress as? MutableStateFlow<Float>)
                    if (emitter != null) {
                        emitter.emit(state.progress)
                        return@LaunchedEffect
                    }
                }
                snackbarDownloadProgressJob =
                    coroutine.launch {
                        snackbar.showSnackbar(
                            getString(Res.string.downloading_model_para),
                            SnackbarExtension.ProgressBar(MutableStateFlow(0f)),
                            SnackbarExtension.Identifier(FeiStatusBarDownloadId),
                            duration = SnackbarDuration.Indefinite
                        )
                    }
            }

            is FeiDbState.InProgress -> {
                if (snackbar.extensions.any { it is SnackbarExtension.Identifier<*> && it.id == FeiStatusBarInferenceId }) {
                    val emitter =
                        (snackbar.extensions.filterFirstIsInstanceOrNull<SnackbarExtension.ProgressBar>()
                            ?.progress as? MutableStateFlow<Float>)
                    if (emitter != null) {
                        emitter.emit(state.done.toFloat() / state.total)
                        return@LaunchedEffect
                    }
                }
                snackbarInferenceProgressJob =
                    coroutine.launch {
                        snackbar.showSnackbar(
                            getPluralString(
                                Res.plurals.inferring_n_items_para,
                                state.total,
                                state.total
                            ),
                            SnackbarExtension.Identifier(FeiStatusBarInferenceId),
                            SnackbarExtension.ProgressBar(MutableStateFlow(state.done.toFloat() / state.total)),
                            duration = SnackbarDuration.Indefinite
                        )
                    }
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

    when (val dialog = downloadDialog) {
        is PendingDownloadDialog.Proceed -> {
            BasicAlertDialog(
                onDismissRequest = {
                    downloadDialog = PendingDownloadDialog.Hidden
                }
            ) {
                Card {
                    DialogContentSkeleton(
                        icon = {
                            Icon(
                                painterResource(Res.drawable.baseline_cloud_download),
                                contentDescription = null,
                                modifier = Modifier.padding(top = PaddingBig)
                            )
                        },
                        title = {
                            Text(stringResource(Res.string.download_required_para))
                        }
                    ) {
                        val totalDownload = remember(dialog.files) {
                            HumanReadable.fileSize(dialog.files.sumOf {
                                it.size ?: (1 shr 12).toLong()
                            })
                        }
                        Text(
                            pluralStringResource(
                                Res.plurals.about_to_download_x_items_will_cost_y_of_data_when_to_para,
                                dialog.files.size,
                                dialog.files.size,
                                totalDownload
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = PaddingBig),
                        )
                        Column {
                            TextButton(
                                onClick = {
                                    coroutine.launch {
                                        dialog.response.send(PendingDownloadResponse.Discretion)
                                        downloadDialog = PendingDownloadDialog.Hidden
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape
                            ) {
                                Text(stringResource(Res.string.using_wifi_only_para))
                            }
                            HorizontalSeparator()
                            TextButton(
                                onClick = {
                                    coroutine.launch {
                                        dialog.response.send(PendingDownloadResponse.Immediate)
                                        downloadDialog = PendingDownloadDialog.Hidden
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape
                            ) {
                                Text(stringResource(Res.string.now_para))
                            }
                            HorizontalSeparator()
                            TextButton(
                                onClick = {
                                    downloadDialog = PendingDownloadDialog.Hidden
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape
                            ) {
                                Text(stringResource(Res.string.cancel_para))
                            }
                        }
                    }
                }
            }
        }

        PendingDownloadDialog.Hidden -> {}
    }
}

sealed class FeiStatusBarId
data object FeiStatusBarDefaultId : FeiStatusBarId()
data object FeiStatusBarInferenceId : FeiStatusBarId()
data object FeiStatusBarDownloadId : FeiStatusBarId()

private sealed class MissingModelDialog {
    data object Hidden : MissingModelDialog()
    data class Shown(val data: FeiDbState.MissingModel) : MissingModelDialog()
}

private sealed class PendingDownloadDialog {
    data object Hidden : PendingDownloadDialog()
    data class Proceed(
        val files: List<DownloadableFile>,
        val response: SendChannel<PendingDownloadResponse>,
    ) : PendingDownloadDialog()
}

