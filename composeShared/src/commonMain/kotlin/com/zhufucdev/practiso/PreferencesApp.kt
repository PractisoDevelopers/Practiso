package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.ActionText
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.DialogWithTextInput
import com.zhufucdev.practiso.composable.FooterColumn
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionSkeletonDefaults
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedElementTransitionKey
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.TextAction
import com.zhufucdev.practiso.composable.modelFeatureString
import com.zhufucdev.practiso.composition.BottomUpComposableScope
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.datamodel.intFlagSetOf
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigation.Goto
import com.zhufucdev.practiso.platform.NavigationOption.OpenQrCode
import com.zhufucdev.practiso.platform.NavigationOption.ScanQrCodeFilter
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import com.zhufucdev.practiso.service.CommunityIdentity
import com.zhufucdev.practiso.service.communityServerEndpoint
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.PreferencesAppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect
import opacity.client.HttpStatusAssertionException
import opacity.client.OpacityClient
import opacity.client.Whoami
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.account_removed_para
import resources.answering_para
import resources.authorization_token_para
import resources.baseline_key_chain
import resources.cancel_para
import resources.clear_para
import resources.clearing_credentials_wont_remove_activities_para
import resources.close_para
import resources.community_para
import resources.compatibility_mode_para
import resources.confirm_para
import resources.continue_para
import resources.credentials_para
import resources.custom_server_for_third_party_access_para
import resources.custom_server_url_para
import resources.device_name_para
import resources.export_para
import resources.frame_embedding_model_para
import resources.import_para
import resources.known_model_names_title
import resources.navigate_up_para
import resources.network_unavailable_para
import resources.no_tokens_para
import resources.not_specified_para
import resources.outline_eye
import resources.outline_eye_off
import resources.outline_server_network
import resources.overwriting_current_token_para
import resources.owner_id_para
import resources.owner_name_para
import resources.proceed_anyway_para
import resources.retry_para
import resources.reveals_accuracy_gets_feedback_for_wrong_answers_para
import resources.settings_para
import resources.show_accuracy_para
import resources.smart_recommendations_para
import resources.stored_access_tokens_and_account_info_to_current_server_para
import resources.use_custom_server_para
import resources.use_only_cpu_for_inference_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesApp(model: SettingsModel = AppSettings, vm: PreferencesAppViewModel = viewModel()) {
    val coroutine = rememberCoroutineScope()

    BottomUpComposableScope { buc ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_para)) },
                    navigationIcon = {
                        PlainTooltipBox(stringResource(Res.string.navigate_up_para)) {
                            IconButton(onClick = {
                                coroutine.launch {
                                    Navigator.navigate(Navigation.Backward)
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(Modifier.padding(paddingValues), state = vm.listState) {
                /* Answering */
                item {
                    SectionCaption(
                        stringResource(Res.string.answering_para),
                        Modifier.padding(horizontal = PaddingBig)
                    )
                }
                item {
                    val value: Boolean by model.showAccuracy.collectAsState()
                    Preference(
                        label = stringResource(Res.string.show_accuracy_para),
                        preview = stringResource(Res.string.reveals_accuracy_gets_feedback_for_wrong_answers_para),
                        tailing = {
                            Switch(
                                checked = value,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            model.showAccuracy.tryEmit(!value)
                        }
                    )
                }

                /* Smart Recommendations */
                item {
                    SectionCaption(
                        stringResource(Res.string.smart_recommendations_para),
                        Modifier.padding(horizontal = PaddingBig).padding(top = PaddingBig)
                    )
                }

                item {
                    val value by model.feiModelIndex.collectAsState()
                    SharedElementTransitionPopup(
                        key = "fei_model_preference",
                        popup = {
                            FeiModelSelector(
                                selection = value,
                                onSelect = model.feiModelIndex::tryEmit
                            )
                        },
                        sharedElement = {
                            PreferenceSkeleton(
                                label = stringResource(Res.string.frame_embedding_model_para),
                                preview = stringArrayResource(Res.array.known_model_names_title)[value],
                                modifier = it
                            )
                        },
                        content = {
                            Preference(
                                label = stringResource(Res.string.frame_embedding_model_para),
                                preview = stringArrayResource(Res.array.known_model_names_title)[value],
                                onClick = {
                                    coroutine.launch {
                                        expand()
                                    }
                                },
                                modifier = Modifier.sharedElement()
                            )
                        }
                    )
                }

                item {
                    val value by model.feiCompatibilityMode.collectAsState()
                    Preference(
                        label = stringResource(Res.string.compatibility_mode_para),
                        preview = stringResource(Res.string.use_only_cpu_for_inference_para),
                        tailing = {
                            Switch(
                                checked = value,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            model.feiCompatibilityMode.tryEmit(!value)
                        }
                    )
                }

                /* Community */
                item {
                    SectionCaption(
                        stringResource(Res.string.community_para),
                        Modifier.padding(horizontal = PaddingBig).padding(top = PaddingBig)
                    )
                }
                item {
                    val value by model.communityUseCustomServer.collectAsState()
                    Preference(
                        label = stringResource(Res.string.use_custom_server_para),
                        preview = stringResource(Res.string.custom_server_for_third_party_access_para),
                        tailing = {
                            Switch(checked = value, onCheckedChange = null)
                        },
                        onClick = {
                            model.communityUseCustomServer.tryEmit(!value)
                        }
                    )
                }
                item {
                    val value by model.communityServerUrl.collectAsState()
                    val enabled by model.communityUseCustomServer.collectAsState()
                    val coroutine = rememberCoroutineScope()
                    SharedElementTransitionPopup(
                        "custom_community_server_url",
                        popup = {
                            var urlBuffer by remember { mutableStateOf(value ?: "") }
                            DialogWithTextInput(
                                icon = {
                                    Icon(
                                        painterResource(Res.drawable.outline_server_network),
                                        contentDescription = null
                                    )
                                },
                                title = {
                                    Text(stringResource(Res.string.custom_server_url_para))
                                },
                                inputValue = urlBuffer,
                                onInputValueChange = {
                                    urlBuffer = it
                                },
                                positiveButton = {
                                    Button(
                                        onClick = {
                                            coroutine.launch {
                                                model.communityServerUrl.emit(
                                                    urlBuffer.takeIf(
                                                        String::isNotBlank
                                                    )
                                                )
                                                collapse()
                                            }
                                        }
                                    ) {
                                        Text(stringResource(Res.string.confirm_para))
                                    }
                                },
                                negativeButton = {
                                    OutlinedButton(
                                        onClick = {
                                            coroutine.launch { collapse() }
                                        }
                                    ) {
                                        Text(stringResource(Res.string.cancel_para))
                                    }
                                }
                            )
                        },
                        content = {
                            Preference(
                                label = stringResource(Res.string.custom_server_url_para),
                                preview = value ?: stringResource(Res.string.not_specified_para),
                                onClick = {
                                    coroutine.launch { expand() }
                                },
                                enabled = enabled,
                                modifier = Modifier.sharedElement()
                            )
                        },
                        sharedElement = {
                            Preference(
                                label = stringResource(Res.string.custom_server_url_para),
                                preview = value ?: stringResource(Res.string.not_specified_para),
                                onClick = {},
                                modifier = it
                            )
                        }
                    )
                }
                item {
                    val serverEndpoint by model.communityServerEndpoint.collectAsState(null)
                    val identity = remember(model, serverEndpoint) {
                        model.getCommunityIdentity(
                            serverEndpoint ?: DEFAULT_COMMUNITY_SERVER_URL
                        )
                    }
                    SharedElementTransitionPopup(
                        key = "community_identity",
                        dismissGestureEnabled = false,
                        popup = {
                            CommunityIdentityDialog(
                                modifier = Modifier.safeContentPadding().pseudoClickable(),
                                model = viewModel {
                                    CommunityIdentityViewModel(
                                        identity = identity,
                                        serverEndpoint = model.communityServerEndpoint,
                                    )
                                },
                                onDismissRequest = { coroutine.launch { collapse() } }
                            )
                        },
                        sharedElement = {
                            Preference(
                                label = stringResource(Res.string.credentials_para),
                                preview = stringResource(Res.string.stored_access_tokens_and_account_info_to_current_server_para),
                                onClick = {},
                                modifier = it
                            )
                        },
                        content = {
                            Preference(
                                label = stringResource(Res.string.credentials_para),
                                preview = stringResource(Res.string.stored_access_tokens_and_account_info_to_current_server_para),
                                onClick = {
                                    coroutine.launch { expand() }
                                },
                                modifier = Modifier.sharedElement()
                            )
                        }
                    )
                }
            }
        }

        buc.compose(SharedElementTransitionKey)
    }
}

@Composable
private fun Preference(
    modifier: Modifier = Modifier,
    label: String,
    preview: String? = null,
    tailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    PreferenceSkeleton(
        modifier = Modifier.clickable(onClick = onClick, enabled = enabled) then modifier,
        label = label,
        preview = preview,
        tailing = tailing,
        enabled = enabled
    )
}

@Composable
fun PreferenceSkeleton(
    modifier: Modifier = Modifier,
    label: String,
    preview: String? = null,
    tailing: @Composable () -> Unit = {},
    enabled: Boolean = true,
) {
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
                horizontal = PaddingBig,
                vertical = PaddingNormal
            )
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.let {
                    if (enabled) {
                        it
                    } else {
                        it.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                PractisoOptionSkeleton(
                    modifier = Modifier.weight(1f),
                    label = { Text(label) },
                    preview = {
                        if (preview != null) {
                            Text(preview)
                        }
                    })
                tailing()
            }
        }
    }
}

@Composable
private fun MlModelSelection(
    modifier: Modifier = Modifier,
    featureContainerModifier: Modifier = Modifier,
    model: MlModel,
    index: Int = KnownModels.indexOf(model),
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    Column {
        Box(Modifier.clickable {
            onSelectedChange(!selected)
        }) {
            Row(modifier) {
                Text(
                    stringArrayResource(Res.array.known_model_names_title)[index],
                    modifier = Modifier.weight(1f)
                )

                RadioButton(
                    selected = selected,
                    onClick = null
                )
            }
        }
        AnimatedVisibility(visible = selected) {
            @Suppress("SimplifiableCallChain") // this cannot be simplified because joinToString is not Composable
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = featureContainerModifier
            ) {
                model.features.map { modelFeatureString(it) }.sortedBy { it.length }
                    .forEach { featureText ->
                        Box(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(PaddingSmall)
                            )
                        ) {
                            Text(
                                featureText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(PaddingSmall)
                            )
                        }
                    }
            }
        }
    }
}


@Composable
private fun FeiModelSelector(
    modifier: Modifier = Modifier,
    selection: Int,
    onSelect: (Int) -> Unit,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(vertical = PaddingBig)) {
            Text(
                stringResource(Res.string.frame_embedding_model_para),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingNormal)
                    .padding(horizontal = PaddingBig)
            )
            KnownModels.forEachIndexed { index, mlModel ->
                MlModelSelection(
                    modifier = Modifier.padding(
                        vertical = PaddingNormal,
                        horizontal = PaddingBig
                    ),
                    featureContainerModifier = Modifier.padding(
                        horizontal = PaddingBig
                    ),
                    model = mlModel,
                    index = index,
                    selected = selection == index,
                    onSelectedChange = {
                        onSelect(index)
                    }
                )
            }
        }
    }
}

sealed class CommunityIdentityDialogState {
    object Empty : CommunityIdentityDialogState()
    data class Loading(val token: AuthorizationToken) : CommunityIdentityDialogState()
    object Lost : CommunityIdentityDialogState()
    data class ConnectionError(val token: AuthorizationToken, val retry: SendChannel<Unit>) :
        CommunityIdentityDialogState()

    data class Loaded(val token: AuthorizationToken, val whoami: Whoami) :
        CommunityIdentityDialogState()

    data class WarnBeforeClear(val proceedClear: SendChannel<Boolean>) :
        CommunityIdentityDialogState()

    data class WarnBeforeImport(
        val token: AuthorizationToken,
        val proceedImport: SendChannel<Boolean>
    ) : CommunityIdentityDialogState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun CommunityIdentityDialog(
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
                    ScanQrCodeFilter(
                        allowedTypes = intFlagSetOf(BarcodeType.AUTHORIZATION_TOKEN)
                    )
                )
            } catch (_: CancellationException) {
                null
            }
        val protocol =
            Protocol(urlString = scanResult ?: return)
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
                }
            }
        }
    }
}

private class CommunityIdentityViewModel(
    identity: CommunityIdentity,
    serverEndpoint: Flow<String>,
) : ViewModel() {
    val clearRequest = Channel<Unit>()
    val importRequest = Channel<AuthorizationToken>()
    private val retryCounter = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CommunityIdentityDialogState> = retryCounter
        .combine(serverEndpoint, ::Pair)
        .combineTransform(identity.authToken) { (_, endpoint), token ->
            emit(
                if (token != null) {
                    CommunityIdentityDialogState.Loading(token)
                } else {
                    CommunityIdentityDialogState.Empty
                }
            )

            val opacityClient = OpacityClient(endpoint, token.toString(), PlatformHttpClientFactory)
            val whoamiChannel = Channel<Result<Whoami?>>()
            coroutineScope {
                launch {
                    if (token != null) {
                        whoamiChannel.send(runCatching { opacityClient.getWhoami() })
                    }
                }
                whileSelect {
                    whoamiChannel.onReceive {
                        if (it.isSuccess) {
                            val whoami = it.getOrThrow()
                            if (whoami != null) {
                                emit(CommunityIdentityDialogState.Loaded(token!!, whoami))
                                true
                            } else {
                                emit(CommunityIdentityDialogState.Lost)
                                false // end state
                            }
                        } else if (it.exceptionOrNull() is HttpStatusAssertionException) {
                            emit(CommunityIdentityDialogState.Lost)
                            false // end state
                        } else {
                            val retry = Channel<Unit>()
                            emit(CommunityIdentityDialogState.ConnectionError(token!!, retry))
                            retry.receive()
                            retryCounter.value++
                            false // end state
                        }
                    }
                    clearRequest.onReceive {
                        if (token == null) {
                            return@onReceive true
                        }
                        val proceed = Channel<Boolean>()
                        val stateBeforeRequest = state.value
                        emit(CommunityIdentityDialogState.WarnBeforeClear(proceed))
                        if (proceed.receive()) {
                            identity.clear()
                            false // end state
                        } else {
                            emit(stateBeforeRequest)
                            true
                        }
                    }
                    importRequest.onReceive { newToken ->
                        if (token == null) {
                            identity.authToken.emit(newToken)
                            false
                        } else {
                            val proceed = Channel<Boolean>()
                            val stateBeforeImport = state.value
                            emit(CommunityIdentityDialogState.WarnBeforeImport(newToken, proceed))
                            if (proceed.receive()) {
                                identity.authToken.emit(newToken)
                                retryCounter.value++
                                false
                            } else {
                                emit(stateBeforeImport)
                                true
                            }
                        }
                    }
                }
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = identity.authToken.value
                ?.let(CommunityIdentityDialogState::Loading)
                ?: CommunityIdentityDialogState.Empty,
        )
}

@Composable
private fun WhoamiInfoRows(model: Whoami) {
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