package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.zhufucdev.practiso.Protocol
import com.zhufucdev.practiso.ProtocolAction
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.intFlagSetOf
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation.Goto
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.NavigationOption.OpenQrCode
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.CommunityIdentityDialogState
import com.zhufucdev.practiso.viewmodel.CommunityIdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import opacity.client.Whoami
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.account_removed_para
import resources.authorization_token_para
import resources.baseline_key_chain
import resources.cancel_para
import resources.clear_para
import resources.clearing_credentials_wont_remove_activities_para
import resources.close_para
import resources.confirm_para
import resources.continue_para
import resources.credentials_para
import resources.device_name_para
import resources.export_para
import resources.import_para
import resources.network_unavailable_para
import resources.no_tokens_para
import resources.no_useful_information_para
import resources.outline_eye
import resources.outline_eye_off
import resources.overwriting_current_token_para
import resources.owner_id_para
import resources.owner_name_para
import resources.proceed_anyway_para
import resources.retry_para
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CommunityIdentityDialog(
    modifier: Modifier = Modifier,
    model: CommunityIdentityViewModel,
    onDismissRequest: () -> Unit
) {
    var revealToken by rememberSaveable { mutableStateOf(false) }

    suspend fun import() {
        val scanResult =
            try {
                Navigator.navigateForResult(
                    Goto(AppDestination.QrCodeScanner),
                    NavigationOption.ScanQrCodeFilter(
                        allowedTypes = intFlagSetOf(BarcodeType.AUTHORIZATION_TOKEN)
                    )
                )
            } catch (_: CancellationException) {
                null
            }
        if (scanResult == null) {
            model.importRequest.send(null)
            return
        }
        val protocol = try {
            Protocol(urlString = scanResult)
        } catch (_: Exception) {
            model.importRequest.send(null)
            return
        }
        when (val action = protocol.action) {
            is ProtocolAction.ImportAuthToken -> {
                model.importRequest.send(action.token)
            }

            else -> {
                // TODO handle action mismatch here
            }
        }
    }

    Card(modifier = modifier) {
        DialogContentSkeleton(
            modifier = Modifier.fillMaxWidth().padding(PaddingBig),
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.baseline_key_chain),
                    contentDescription = null
                )
            },
            title = {
                Text(stringResource(Res.string.credentials_para))
            }
        ) {
            val state by model.state.collectAsState()
            AnimatedContent(state) { state ->
                when (state) {
                    CommunityIdentityDialogState.Empty -> FooterColumn {
                        ActionText(
                            text = stringResource(Res.string.no_tokens_para),
                            actions = listOf(
                                TextAction(
                                    label = stringResource(Res.string.import_para),
                                    action = {
                                        MainScope().launch {
                                            import()
                                        }
                                    }
                                )
                            )
                        )
                        Spacer(Modifier.height(PaddingNormal))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = onDismissRequest) {
                                Text(stringResource(Res.string.close_para))
                            }
                        }
                    }

                    is CommunityIdentityDialogState.Loaded ->
                        FooterColumn {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(PaddingNormal),
                                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                            ) {
                                AuthorizationTokenOption(
                                    reveal = revealToken,
                                    onRevealStateChange = { revealToken = it },
                                    model = state.token,
                                    onImportRequest = {
                                        MainScope().launch {
                                            import()
                                        }
                                    }
                                )
                                WhoamiInfoRows(state.whoami)
                            }

                            Spacer(Modifier.height(PaddingNormal))
                            ClearTokenRow(
                                onDismissRequest = onDismissRequest,
                                onClearRequest = { model.clearRequest.trySend(Unit) }
                            )
                        }

                    is CommunityIdentityDialogState.Loading ->
                        FooterColumn {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(PaddingNormal),
                                modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                            ) {
                                AuthorizationTokenOption(
                                    reveal = revealToken,
                                    onRevealStateChange = { revealToken = it },
                                    model = state.token,
                                    onImportRequest = {
                                        MainScope().launch {
                                            import()
                                        }
                                    }
                                )
                                WhoamiInfoRowsSkeleton()
                            }

                            Spacer(Modifier.height(PaddingNormal))
                            ClearTokenRow(
                                onDismissRequest = onDismissRequest,
                                onClearRequest = { model.clearRequest.trySend(Unit) }
                            )
                        }

                    CommunityIdentityDialogState.Lost -> {
                        FooterColumn {
                            Text(stringResource(Res.string.account_removed_para))
                            Spacer(Modifier.height(PaddingNormal))
                            ClearTokenRow(
                                onDismissRequest = onDismissRequest,
                                onClearRequest = { model.clearRequest.trySend(Unit) }
                            )
                        }
                    }

                    is CommunityIdentityDialogState.WarnBeforeClear ->
                        Column {
                            Text(stringResource(Res.string.clearing_credentials_wont_remove_activities_para))
                            Spacer(Modifier.height(PaddingNormal))
                            Row {
                                OutlinedButton(onClick = {
                                    state.proceedClear.trySend(false)
                                }) {
                                    Text(stringResource(Res.string.cancel_para))
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        state.proceedClear.trySend(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(stringResource(Res.string.proceed_anyway_para))
                                }
                            }
                        }

                    is CommunityIdentityDialogState.WarnBeforeImport -> Column {
                        Text(stringResource(Res.string.overwriting_current_token_para))
                        Spacer(Modifier.height(PaddingNormal))
                        Row {
                            OutlinedButton(onClick = {
                                state.proceedImport.trySend(false)
                            }) {
                                Text(stringResource(Res.string.cancel_para))
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    state.proceedImport.trySend(true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text(stringResource(Res.string.continue_para))
                            }
                        }
                    }

                    is CommunityIdentityDialogState.ConnectionError -> FooterColumn {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(PaddingNormal),
                            modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                        ) {
                            AuthorizationTokenOption(
                                reveal = revealToken,
                                onRevealStateChange = { revealToken = it },
                                model = state.token,
                                onImportRequest = {
                                    MainScope().launch {
                                        import()
                                    }
                                }
                            )
                            Text(stringResource(Res.string.network_unavailable_para))
                        }
                        Spacer(Modifier.height(PaddingNormal))
                        Row {
                            OutlinedButton(onClick = {
                                state.retry.trySend(Unit)
                            }) {
                                Text(stringResource(Res.string.retry_para))
                            }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = onDismissRequest) {
                                Text(stringResource(Res.string.close_para))
                            }
                        }
                    }

                    is CommunityIdentityDialogState.NoUsefulInformation -> {
                        Column {
                            Text(stringResource(Res.string.no_useful_information_para))
                            Spacer(Modifier.height(PaddingNormal))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = {
                                    state.back.trySend(Unit)
                                }) {
                                    Text(stringResource(Res.string.confirm_para))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorizationTokenOption(
    reveal: Boolean,
    onRevealStateChange: (Boolean) -> Unit,
    onImportRequest: () -> Unit,
    model: AuthorizationToken
) {
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.authorization_token_para)) },
        preview = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionText(
                    modifier = Modifier.weight(1f),
                    text = model.toString(),
                    visualTransformation = if (!reveal) PasswordVisualTransformation() else VisualTransformation.None,
                    actions = listOf(
                        TextAction(
                            label = stringResource(Res.string.export_para),
                            action = {
                                MainScope().launch {
                                    Navigator.navigate(
                                        Goto(AppDestination.QrCodeViewer),
                                        OpenQrCode(
                                            stringValue =
                                                Protocol.importAuthToken(
                                                    model
                                                ).toString(),
                                        )
                                    )
                                }
                            }
                        ),
                        TextAction(
                            label = stringResource(Res.string.import_para),
                            action = onImportRequest
                        )
                    )
                )
                IconButton(
                    onClick = {
                        onRevealStateChange(!reveal)
                    }
                ) {
                    Icon(
                        painter = painterResource(if (!reveal) Res.drawable.outline_eye else Res.drawable.outline_eye_off),
                        contentDescription = null,
                    )
                }
            }
        }
    )
}

@Composable
private fun ClearTokenRow(onDismissRequest: () -> Unit, onClearRequest: () -> Unit) {
    Row {
        Button(
            onClick = onClearRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(stringResource(Res.string.clear_para))
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onDismissRequest) {
            Text(stringResource(Res.string.close_para))
        }
    }
}

@Composable
private fun WhoamiInfoRowsSkeleton(
    deviceName: @Composable ColumnScope.() -> Unit = { PractisoOptionSkeletonDefaults.Preview() },
    ownerName: @Composable ColumnScope.() -> Unit = { PractisoOptionSkeletonDefaults.Preview() },
    ownerId: @Composable ColumnScope.() -> Unit = { PractisoOptionSkeletonDefaults.Preview() },
) {
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.device_name_para)) },
        preview = deviceName
    )
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.owner_name_para)) },
        preview = ownerName
    )
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.owner_id_para)) },
        preview = ownerId
    )
}

@Composable
fun WhoamiInfoRows(model: Whoami) {
    WhoamiInfoRowsSkeleton(
        deviceName = {
            Text(model.clientName)
        },
        ownerName = {
            Text(model.name ?: model.clientName)
        },
        ownerId = {
            Text(model.ownerId.toString())
        }
    )
}

