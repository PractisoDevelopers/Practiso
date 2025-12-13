package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.DEFAULT_COMMUNITY_SERVER_URL
import com.zhufucdev.practiso.Protocol
import com.zhufucdev.practiso.ProtocolAction
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.center
import com.zhufucdev.practiso.datamodel.height
import com.zhufucdev.practiso.datamodel.width
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import com.zhufucdev.practiso.platform.isDebugBuild
import com.zhufucdev.practiso.style.PaddingNormal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import opacity.client.OpacityClient
import opacity.client.Whoami
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_key_chain
import resources.credentials_para
import resources.invalid_authorization_token_para
import resources.no_useful_information_para
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BarcodeOverlay(
    modifier: Modifier = Modifier,
    barcodes: List<Barcode>,
    state: BarcodeOverlayState,
    onClickBarcode: ((Barcode) -> Unit)? = null
) {
    assert(state is BarcodeOverlayStateImpl) { "External implementation of BarcodeOverlayState is not allowed" }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(coroutineScope, state) {
        (state as BarcodeOverlayStateImpl).coroutineScope = coroutineScope
    }

    SharedTransitionScope { trans ->
        AnimatedContent(barcodes, modifier = trans then modifier, transitionSpec = {
            (fadeIn(animationSpec = tween(220)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        }
        ) { barcodes ->
            Layout(contents = barcodes.map { barcode ->
                when (barcode.type) {
                    BarcodeType.AUTHORIZATION_TOKEN -> ({
                        val token =
                            (Protocol(urlString = barcode.value).action as ProtocolAction.ImportAuthToken).token
                        val whoami by produceState((state as BarcodeOverlayStateImpl).whoamiCache[token.toString()]) {
                            value =
                                state.getWhoamiResult(token)
                        }
                        AuthTokenOverlay(
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = barcode.value),
                                this
                            ), model = whoami, onClick = {
                                onClickBarcode?.invoke(barcode)
                            })
                    })

                    else -> ({
                        UnknownBarcodeOverlay(
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = barcode.value),
                                this
                            ), onClick = { onClickBarcode?.invoke(barcode) })
                    })
                }
            }) { measureables, constraints ->
                val placeables = measureables.mapIndexed { index, m ->
                    val barcode = barcodes[index]
                    m.first().measure(
                        Constraints(
                            minWidth = maxOf(
                                minOf(
                                    (barcode.width * 0.85).roundToInt(),
                                    constraints.maxWidth
                                ),
                                0
                            ),
                            maxHeight = maxOf(
                                minOf(
                                    (barcode.height * 0.85).roundToInt(),
                                    constraints.maxHeight
                                ),
                                0
                            )
                        )
                    ) to barcode.center
                }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEachIndexed { index, (placeable, anchor) ->
                        placeable.place(
                            x = anchor.x - placeable.width / 2,
                            y = anchor.y - placeable.height / 2,
                            zIndex = index.toFloat()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnknownBarcodeOverlay(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    ElevatedCard(modifier, onClick = onClick) {
        Box(Modifier.padding(PaddingNormal)) {
            Text(stringResource(Res.string.no_useful_information_para))
        }
    }
}

@Composable
private fun AuthTokenOverlay(
    modifier: Modifier = Modifier,
    model: Result<Whoami>?,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(modifier then Modifier.width(IntrinsicSize.Max), onClick = onClick) {
        Row(
            Modifier.padding(PaddingNormal),
            horizontalArrangement = Arrangement.spacedBy(PaddingNormal)
        ) {
            Icon(
                painterResource(Res.drawable.baseline_key_chain),
                contentDescription = stringResource(Res.string.credentials_para)
            )
            if (model == null) {
                PractisoOptionSkeleton(Modifier.weight(1f))
                return@Row
            }
            val whoami = model.getOrNull()
            if (whoami != null) {
                PractisoOptionSkeleton(
                    label = {
                        Text(whoami.name ?: whoami.clientName)
                    },
                    preview = {
                        Text(whoami.clientName)
                    }
                )
            } else if (isDebugBuild()) {
                Column {
                    Text(stringResource(Res.string.invalid_authorization_token_para))
                    val err = model.exceptionOrNull()!!
                    Text("${err::class.simpleName}: ${err.message}")
                }
            } else {
                Text(stringResource(Res.string.invalid_authorization_token_para))
            }
        }
    }
}

@Composable
private fun ElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            content = content
        )
    }
}

interface BarcodeOverlayState {
    suspend fun getWhoamiResult(authToken: AuthorizationToken): Result<Whoami>
}

fun BarcodeOverlayState(serverUrl: String = DEFAULT_COMMUNITY_SERVER_URL): BarcodeOverlayState =
    BarcodeOverlayStateImpl(serverUrl)

@Stable
private class BarcodeOverlayStateImpl(val serverUrl: String) : BarcodeOverlayState {
    lateinit var coroutineScope: CoroutineScope
    val whoamiCache = mutableStateMapOf<String, Result<Whoami>>()
    private val fifo = mutableListOf<String>()
    override suspend fun getWhoamiResult(authToken: AuthorizationToken): Result<Whoami> =
        coroutineScope.async {
            getWhoamiResultImpl(authToken.toString())
        }.await()

    suspend fun getWhoamiResultImpl(authToken: String): Result<Whoami> {
        val cache = whoamiCache[authToken]
        if (cache != null) {
            return cache
        }
        val opacityClient = OpacityClient(serverUrl, authToken, PlatformHttpClientFactory)
        val queryResult = runCatching { opacityClient.getWhoami()!! }
        whoamiCache[authToken] = queryResult
        fifo.add(authToken)
        if (whoamiCache.size > MAX_CACHE_CAPACITY) {
            whoamiCache.remove(fifo.removeAt(0))
        }
        return queryResult
    }

    companion object {
        const val MAX_CACHE_CAPACITY = 10
    }
}