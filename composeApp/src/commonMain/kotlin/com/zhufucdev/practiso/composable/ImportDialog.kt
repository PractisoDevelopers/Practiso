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
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.viewmodel.ErrorModel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_alert_box_outline
import practiso.composeapp.generated.resources.baseline_import
import practiso.composeapp.generated.resources.cancel_para
import practiso.composeapp.generated.resources.continue_para
import practiso.composeapp.generated.resources.dismiss_para
import practiso.composeapp.generated.resources.ignore_para
import practiso.composeapp.generated.resources.import_from_practiso_archive_para
import practiso.composeapp.generated.resources.importing_x_items_y_done
import practiso.composeapp.generated.resources.retry_para
import practiso.composeapp.generated.resources.skip_para
import practiso.composeapp.generated.resources.unarchiving_this_file_ellipsis_para
import practiso.composeapp.generated.resources.will_import_n_items_to_library

sealed interface ImportState {
    data object Idle : ImportState
    data class Unarchiving(val target: String) : ImportState
    data class Confirmation(
        val total: Int,
        val ok: SendChannel<Unit>,
        val dismiss: SendChannel<Unit>,
    ) : ImportState

    data class Importing(val total: Int, val done: Int) : ImportState

    data class Error(
        val model: ErrorModel,
        val cancel: SendChannel<Unit>,
        val retry: SendChannel<Unit>? = null,
        val skip: SendChannel<Unit>? = null,
        val ignore: SendChannel<Unit>? = null,
    ) : ImportState
}

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
                        ImportState.Idle,
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
                val error = state.model
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
                    if (state.skip != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.skip.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.skip_para))
                        }
                    }
                    if (state.ignore != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.ignore.send(Unit)
                                }
                            },
                        ) {
                            Text(stringResource(Res.string.ignore_para))
                        }
                    }
                    if (state.retry != null) {
                        PrimaryButton(
                            onClick = {
                                coroutine.launch {
                                    state.retry.send(Unit)
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

