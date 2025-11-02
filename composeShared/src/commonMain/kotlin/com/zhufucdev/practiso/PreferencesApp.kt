package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.DialogContentSkeleton
import com.zhufucdev.practiso.composable.DialogWithTextInput
import com.zhufucdev.practiso.composable.FooterColumn
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.PractisoOptionSkeletonDefaults
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedElementTransitionKey
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.modelFeatureString
import com.zhufucdev.practiso.composition.BottomUpComposableScope
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import com.zhufucdev.practiso.service.CommunityIdentity
import com.zhufucdev.practiso.service.communityServerEndpoint
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.coroutines.launch
import opacity.client.OpacityClient
import opacity.client.Whoami
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
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
import resources.credentials_para
import resources.custom_server_for_third_party_access_para
import resources.custom_server_url_para
import resources.device_name_para
import resources.frame_embedding_model_para
import resources.known_model_names_title
import resources.navigate_up_para
import resources.no_tokens_para
import resources.not_specified_para
import resources.outline_eye
import resources.outline_eye_off
import resources.outline_server_network
import resources.owner_id_para
import resources.owner_name_para
import resources.proceed_anyway_para
import resources.reveals_accuracy_gets_feedback_for_wrong_answers_para
import resources.settings_para
import resources.show_accuracy_para
import resources.smart_recommendations_para
import resources.stored_access_tokens_and_account_info_to_current_server_para
import resources.use_custom_server_para
import resources.use_only_cpu_for_inference_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesApp(model: SettingsModel = AppSettings) {
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
            LazyColumn(Modifier.padding(paddingValues)) {
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
                    val identity by remember(model) {
                        derivedStateOf {
                            model.getCommunityIdentity(
                                model.communityServerUrl.value ?: DEFAULT_COMMUNITY_SERVER_URL
                            )
                        }
                    }
                    SharedElementTransitionPopup(
                        key = "community_identity",
                        dismissGestureEnabled = false,
                        popup = {
                            val server by model.communityServerEndpoint.collectAsState(null)
                            CommunityIdentityDialog(
                                modifier = Modifier.safeContentPadding().pseudoClickable(),
                                model = identity,
                                serverEndpoint = server,
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

@Composable
private fun CommunityIdentityDialog(
    modifier: Modifier = Modifier,
    model: CommunityIdentity,
    serverEndpoint: String?,
    onDismissRequest: () -> Unit
) {
    val opacityClient = remember(serverEndpoint) {
        serverEndpoint?.let {
            OpacityClient(it, PlatformHttpClientFactory)
        }
    }
    val whoami by produceState<Whoami?>(null, opacityClient) {
        if (opacityClient == null) {
            value = null
            return@produceState
        }
        val token = model.authToken
        if (token == null) {
            value = null
            return@produceState
        }
        value = opacityClient.getWhoami(token)
    }

    var tryingToClear by rememberSaveable { mutableStateOf(false) }
    var revealToken by rememberSaveable { mutableStateOf(false) }

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
            AnimatedContent(tryingToClear) { showClearDialog ->
                if (showClearDialog) {
                    Column {
                        Text(stringResource(Res.string.clearing_credentials_wont_remove_activities_para))
                        Spacer(Modifier.height(PaddingNormal))
                        Row {
                            OutlinedButton(onClick = {
                                tryingToClear = false
                            }) {
                                Text(stringResource(Res.string.cancel_para))
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    model.clear()
                                    onDismissRequest()
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
                } else {
                    FooterColumn {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(PaddingNormal),
                            modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)
                        ) {
                            PractisoOptionSkeleton(
                                label = { Text(stringResource(Res.string.authorization_token_para)) },
                                preview = {
                                    val token = model.authToken
                                    if (token != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            BasicTextField(
                                                value = token,
                                                onValueChange = {},
                                                readOnly = true,
                                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                                                visualTransformation = if (!revealToken) PasswordVisualTransformation() else VisualTransformation.None,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    revealToken = !revealToken
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(if (!revealToken) Res.drawable.outline_eye else Res.drawable.outline_eye_off),
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    } else {
                                        Text(stringResource(Res.string.no_tokens_para))
                                    }
                                }
                            )

                            if (model.authToken != null) {
                                WhoamiInfoRows(whoami)
                            }
                        }

                        Spacer(Modifier.height(PaddingNormal))
                        Row {
                            if (model.authToken != null) {
                                Button(
                                    onClick = { tryingToClear = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(stringResource(Res.string.clear_para))
                                }
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

@Composable
private fun WhoamiInfoRows(model: Whoami?) {
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.device_name_para)) },
        preview = {
            if (model == null) {
                PractisoOptionSkeletonDefaults.Preview()
            } else {
                Text(model.clientName)
            }
        }
    )
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.owner_name_para)) },
        preview = {
            if (model == null) {
                PractisoOptionSkeletonDefaults.Preview()
            } else {
                Text(model.name ?: model.clientName)
            }
        }
    )
    PractisoOptionSkeleton(
        label = { Text(stringResource(Res.string.owner_id_para)) },
        preview = {
            if (model == null) {
                PractisoOptionSkeletonDefaults.Preview()
            } else {
                Text(model.ownerId.toString())
            }
        }
    )
}