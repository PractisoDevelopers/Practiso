package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.DEFAULT_DIMOJI
import com.zhufucdev.practiso.platform.DownloadCycle
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadStopped
import com.zhufucdev.practiso.style.NotoEmojiFontFamily
import com.zhufucdev.practiso.style.PaddingSmall
import opacity.client.ArchiveMetadata
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_cloud_download
import resources.cancel_para
import resources.details_para
import resources.download_and_import_para
import resources.download_error_para
import resources.downloads_para
import resources.likes_para
import resources.outline_download
import resources.outline_heart
import resources.retry_para


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveMetadataOption(
    modifier: Modifier = Modifier,
    model: ArchiveMetadata,
    state: DownloadCycle,
    onDownloadRequest: () -> Unit,
    onCancelRequest: () -> Unit,
    onErrorDetailsRequest: (Exception) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        PractisoOptionSkeleton(
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    model.name.removeSuffix(".psarchive"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            preview = {
                AnimatedContent(state, modifier = Modifier.fillMaxWidth(), transitionSpec = {
                    if (state is DownloadState.Downloading) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        defaultAnimatedContentSpecs
                    }
                }) { state ->
                    when (state) {
                        is DownloadStopped.Idle,
                        is DownloadState.Completed,
                            -> {
                            Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                                FlowRow(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
                                ) {
                                    model.dimensions.take(5).forEach {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                it.emoji ?: DEFAULT_DIMOJI,
                                                fontFamily = NotoEmojiFontFamily()
                                            )
                                            Text(
                                                "${it.name} (${it.quizCount})",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (model.dimensions.size > 5) {
                                        InverseText(
                                            text = "+${model.dimensions.size - 5}",
                                            style = TextStyle(
                                                fontWeight = FontWeight.Black,
                                            ),
                                            margin = PaddingValues(horizontal = PaddingSmall, vertical = 1.dp),
                                            shape = RoundedCornerShape(PaddingSmall)
                                        )
                                    }
                                }

                                FlowRow(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                                ) {
                                    val lineHeight = LocalTextStyle.current.lineHeight.value.dp
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painterResource(Res.drawable.outline_download),
                                            contentDescription = stringResource(Res.string.downloads_para),
                                            modifier = Modifier.size(lineHeight, lineHeight)
                                        )
                                        Text(model.downloads.toString())
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painterResource(Res.drawable.outline_heart),
                                            contentDescription = stringResource(Res.string.likes_para),
                                            modifier = Modifier.size(lineHeight, lineHeight)
                                        )
                                        Text(model.likes.toString())
                                    }
                                }
                            }
                        }

                        is DownloadStopped.Error -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                                Text(stringResource(Res.string.download_error_para))
                                Text(
                                    stringResource(Res.string.details_para),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = {
                                        onErrorDetailsRequest(state.model)
                                    })
                                )
                            }
                        }

                        is DownloadState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        is DownloadState.Preparing,
                        is DownloadState.Configure,
                            -> {
                            LinearProgressIndicator(Modifier.weight(1f))
                        }
                    }
                }
            }
        )
        AnimatedContent(state, transitionSpec = {
            if (state is DownloadState.Downloading) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                defaultAnimatedContentSpecs
            }
        }) { state ->
            Box(Modifier.size(32.dp, 32.dp)) {
                when (state) {
                    is DownloadState.Completed -> IconButton(
                        onClick = onDownloadRequest,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painterResource(Res.drawable.outline_download),
                            contentDescription = stringResource(Res.string.download_and_import_para)
                        )
                    }

                    is DownloadStopped.Idle ->
                        PlainTooltipBox(
                            text = stringResource(Res.string.download_and_import_para)
                        ) {
                            IconButton(
                                onClick = onDownloadRequest,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    painterResource(Res.drawable.baseline_cloud_download),
                                    contentDescription = null
                                )
                            }
                        }

                    is DownloadStopped.Error ->
                        PlainTooltipBox(text = stringResource(Res.string.retry_para)) {
                            IconButton(
                                onClick = onDownloadRequest,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null
                                )
                            }
                        }

                    is DownloadState -> PlainTooltipBox(stringResource(Res.string.cancel_para)) {
                        IconButton(
                            onClick = onCancelRequest,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

private val defaultAnimatedContentSpecs =
    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
        .togetherWith(fadeOut(animationSpec = tween(90)))
