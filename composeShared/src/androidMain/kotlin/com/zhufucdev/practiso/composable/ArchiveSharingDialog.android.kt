package com.zhufucdev.practiso.composable

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.ForActivityResultLaunchable
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.ArchiveSharingViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.action_was_cancelled_para
import resources.baseline_android
import resources.baseline_check_circle_outline
import resources.file_name_para
import resources.retry_para
import resources.sending_para
import resources.share_para
import resources.share_with_other_apps_para
import resources.waiting_for_service_para

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun ArchiveSharingDialog(
    modifier: Modifier,
    model: ArchiveSharingViewModel,
    onDismissRequested: () -> Unit,
) {
    ArchiveSharingDialogScaffold(modifier, model, onDismissRequested) {
        exportToFileOption(model)
        uploadToCommunity(model)
        starterOption(
            "system_share",
            icon = {
                Icon(
                    painterResource(Res.drawable.baseline_android),
                    contentDescription = null
                )
            },
            label = { Text(stringResource(Res.string.share_with_other_apps_para)) }
        ) {
            val hostActivity = LocalActivity.current!!
            LaunchedEffect(model) {
                model.shareWithOtherApps.send(hostActivity as ForActivityResultLaunchable)
            }

            var nameBuffer by rememberSaveable { mutableStateOf("") }

            AnimatedContent(
                model.systemShareState,
                transitionSpec = { fadeIn() togetherWith fadeOut() }) { shareState ->
                Column(
                    Modifier
                        .padding(horizontal = PaddingNormal)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (shareState) {
                        is ArchiveSharingViewModel.SystemShare.Cancelled -> {
                            Text(stringResource(Res.string.action_was_cancelled_para))
                            ActionButtons {
                                Button(onClick = {
                                    model.shareWithOtherApps.trySend(hostActivity as ForActivityResultLaunchable)
                                }) {
                                    Text(stringResource(Res.string.retry_para))
                                }
                            }
                        }

                        is ArchiveSharingViewModel.SystemShare.NamedRequired -> {
                            LaunchedEffect(model) {
                                if (nameBuffer.isEmpty()) {
                                    nameBuffer = model.describeSelection()
                                }
                            }

                            TextField(
                                value = nameBuffer,
                                onValueChange = { nameBuffer = it },
                                label = { Text(stringResource(Res.string.file_name_para)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                            )
                            ActionButtons {
                                Button(
                                    onClick = {
                                        shareState.submission.trySend(nameBuffer)
                                    },
                                    enabled = nameBuffer.isNotBlank()
                                ) {
                                    Text(stringResource(Res.string.share_para))
                                }
                            }
                        }

                        is ArchiveSharingViewModel.SystemShare.Sending -> {
                            CircularWavyProgressIndicator()
                            Text(stringResource(Res.string.sending_para))
                        }

                        null -> {
                            CircularWavyProgressIndicator()
                            Text(stringResource(Res.string.waiting_for_service_para))
                        }

                        ArchiveSharingViewModel.SystemShare.Success -> {
                            Icon(
                                painterResource(Res.drawable.baseline_check_circle_outline),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            LaunchedEffect(true) {
                                dismiss()
                                model.cancel()
                            }
                        }
                    }
                }
            }
        }
    }
}