package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.service.ImportState
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.viewmodel.stringContent
import com.zhufucdev.practiso.viewmodel.stringTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_alert_box_outline
import resources.baseline_import
import resources.cancel_para
import resources.continue_para
import resources.dismiss_para
import resources.ignore_para
import resources.import_from_practiso_archive_para
import resources.importing_x_items_y_done
import resources.retry_para
import resources.skip_para
import resources.unarchiving_this_file_ellipsis_para
import resources.will_import_n_items_to_library

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(state: ImportState) {
    if (state is ImportState.Idle) {
        return
    }
    val coroutine = rememberCoroutineScope()
    BasicAlertDialog(
        onDismissRequest = {},
    ) {
        Card {
            if (state !is ImportState.Error) {
                DialogContentSkeleton(
                    modifier = Modifier.fillMaxWidth().padding(PaddingBig),
                    icon = {
                        Icon(
                            painterResource(Res.drawable.baseline_import),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            stringResource(Res.string.import_from_practiso_archive_para),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                ) {
                    when (state) {
                        is ImportState.Confirmation -> {
                            Text(
                                pluralStringResource(
                                    Res.plurals.will_import_n_items_to_library,
                                    state.total,
                                    state.total
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        is ImportState.Importing -> {
                            Text(
                                pluralStringResource(
                                    Res.plurals.importing_x_items_y_done,
                                    state.total,
                                    state.total,
                                    state.done
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is ImportState.Unarchiving -> {
                            Text(
                                stringResource(Res.string.unarchiving_this_file_ellipsis_para),
                            )
                        }

                        is ImportState.Error,
                        is ImportState.Idle,
                            -> error("Should never reach here")
                    }
                }
                if (state is ImportState.Confirmation) {
                    Column {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.ok.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.continue_para))
                        }
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.dismiss.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.dismiss_para))
                        }
                    }
                }
            } else {
                val error = state.error
                DialogContentSkeleton(
                    modifier = Modifier.fillMaxWidth().padding(PaddingBig),
                    icon = {
                        Icon(
                            painterResource(Res.drawable.baseline_alert_box_outline),
                            contentDescription = null
                        )
                    },
                    title = {
                        Text(error.stringTitle())
                    }
                ) {
                    Text(error.stringContent())
                }
                Column {
                    val skip = state.skip
                    if (skip != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    skip.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.skip_para))
                        }
                    }
                    val ignore = state.ignore
                    if (ignore != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    ignore.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.ignore_para))
                        }
                    }
                    val retry = state.retry
                    if (retry != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    retry.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.retry_para))
                        }
                    }
                    PrimaryButton(
                        onClick = {
                            coroutine.launch {
                                state.cancel.send(Unit)
                            }
                        },
                    ) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    HorizontalSeparator(Modifier.height(2.dp))
    TextButton(
        onClick = onClick,
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

