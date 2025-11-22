package com.zhufucdev.practiso

import com.russhwolf.settings.Settings
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityIdentity
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import okio.Buffer
import okio.Sink
import okio.buffer
import okio.gzip

private const val KeyAnswerPageStyle = "answer_page_style"
private const val KeyShowAccuracy = "show_accuracy"
private const val KeyQuizAutoplay = "quiz_autoplay"
private const val KeyFeiModelId = "fei_model_id"
private const val KeyFeiCompatibility = "fei_compatibility"
private const val KeyCommunityServerUrl = "community_server_url"
private const val KeyCommunityUseCustomServer = "community_use_custom_ser"
private const val KeyClientName = "client_name"

class SettingsModel(
    private val default: Settings,
    private val secure: Settings,
    val coroutineScope: CoroutineScope,
) {
    val answerPageStyle = MutableStateFlow(
        PageStyle.entries[default.getInt(
            KeyAnswerPageStyle,
            PageStyle.Horizontal.ordinal
        )]
    )
    val showAccuracy =
        MutableStateFlow(default.getBoolean(KeyShowAccuracy, false))
    val showNextQuizAutomatically = MutableStateFlow(default.getBoolean(KeyQuizAutoplay, false))
    val feiModelIndex = MutableStateFlow(default.getInt(KeyFeiModelId, defaultValue = 0))
    val feiCompatibilityMode =
        MutableStateFlow(default.getBoolean(KeyFeiCompatibility, defaultValue = false))
    val communityUseCustomServer =
        MutableStateFlow(default.getBoolean(KeyCommunityUseCustomServer, defaultValue = false))
    val communityServerUrl =
        MutableStateFlow(default.getStringOrNull(KeyCommunityServerUrl))
    val clientName =
        MutableStateFlow(default.getStringOrNull(KeyClientName) ?: getPlatform().deviceName)

    fun getCommunityIdentity(serverUrl: String) =
        HybridSettingsCommunityIdentity(serverUrl, coroutineScope, default, secure)

    init {
        with(coroutineScope) {
            launch {
                answerPageStyle.collectLatest {
                    default.putInt(KeyAnswerPageStyle, it.ordinal)
                }
            }
            launch {
                showAccuracy.collectLatest {
                    default.putBoolean(KeyShowAccuracy, it)
                }
            }
            launch {
                showNextQuizAutomatically.collectLatest {
                    default.putBoolean(KeyQuizAutoplay, it)
                }
            }
            launch {
                feiModelIndex.collectLatest {
                    default.putInt(KeyFeiModelId, it)
                }
            }
            launch {
                feiCompatibilityMode.collectLatest {
                    default.putBoolean(KeyFeiCompatibility, it)
                }
            }
            launch {
                communityServerUrl.collectLatest {
                    if (it == null) {
                        default.remove(KeyCommunityServerUrl)
                    } else {
                        default.putString(KeyCommunityServerUrl, it)
                    }
                }
            }
            launch {
                communityUseCustomServer.collectLatest {
                    default.putBoolean(KeyCommunityUseCustomServer, it)
                }
            }
            launch {
                clientName.collectLatest {
                    default.putString(KeyClientName, it)
                }
            }
        }
    }
}

class HybridSettingsCommunityIdentity(
    serverUrl: String,
    coroutineScope: CoroutineScope,
    private val insecure: Settings,
    private val secure: Settings = insecure,
) : CommunityIdentity {
    private val keyPrefix =
        Buffer().apply {
            (Buffer().apply { write(serverUrl.toByteArray()) } as Sink)
                .gzip()
                .buffer()
                .writeAll(this)
        }
            .readByteArray()
            .encodeBase64()


    override val authToken = MutableStateFlow(secure.getStringOrNull("${keyPrefix}_token"))

    override fun clear() {
        authToken.tryEmit(null)
    }

    init {
        coroutineScope.launch {
            authToken.collectLatest { value ->
                val key = "${keyPrefix}_token"
                if (value != null) {
                    secure.putString(key, value)
                } else {
                    secure.remove(key)
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
object AppSettingsScope : CoroutineScope by CoroutineScope(newSingleThreadContext("AppSettings"))

val AppSettings = with(AppSettingsScope) {
    val model = SettingsModel(
        getDefaultSettingsFactory().create(),
        getSecureSettingsFactory().create(),
        this
    )
    launch {
        model.feiModelIndex.collectLatest { modelIdx ->
            Database.fei.setFeiModel(KnownModels[modelIdx])
        }
    }
    model
}
