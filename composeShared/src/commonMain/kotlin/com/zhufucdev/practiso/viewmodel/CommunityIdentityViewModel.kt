package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import com.zhufucdev.practiso.service.CommunityIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class CommunityIdentityViewModel(
    identity: CommunityIdentity,
    serverEndpoint: Flow<String>,
) : ViewModel() {
    val clearRequest = Channel<Unit>()
    val importRequest = Channel<AuthorizationToken?>()
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
                                true
                            }
                        } else if (it.exceptionOrNull() is HttpStatusAssertionException) {
                            emit(CommunityIdentityDialogState.Lost)
                            true
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
                        if (newToken == null) {
                            val back = Channel<Unit>()
                            emit(CommunityIdentityDialogState.NoUsefulInformation(back))
                            back.receive()
                            retryCounter.value++
                            false
                        } else if (token == null) {
                            identity.setAuthToken(newToken)
                            false
                        } else {
                            val proceed = Channel<Boolean>()
                            val stateBeforeImport = state.value
                            emit(CommunityIdentityDialogState.WarnBeforeImport(newToken, proceed))
                            if (proceed.receive()) {
                                identity.setAuthToken(newToken)
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

sealed class CommunityIdentityDialogState {
    object Empty : CommunityIdentityDialogState()
    data class NoUsefulInformation(val back: SendChannel<Unit>) : CommunityIdentityDialogState()
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

