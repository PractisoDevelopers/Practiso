package com.zhufucdev.practiso

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import com.russhwolf.settings.coroutines.getIntOrNullFlow
import com.russhwolf.settings.coroutines.getIntStateFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringStateFlow
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.CommunityIdentity
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

private const val KeyAnswerPageStyle = "answer_page_style"
private const val KeyShowAccuracy = "show_accuracy"
private const val KeyQuizAutoplay = "quiz_autoplay"
private const val KeyFeiModelId = "fei_model_id"
private const val KeyFeiCompatibility = "fei_compatibility"
private const val KeyCommunityServerUrl = "community_server_url"
private const val KeyCommunityUseCustomServer = "community_use_custom_ser"
private const val KeyClientName = "client_name"

@OptIn(ExperimentalSettingsApi::class)
class SettingsModel(
    private val default: ObservableSettings,
    private val secure: ObservableSettings,
    val coroutineScope: CoroutineScope,
) {
    val answerPageStyle = default.getIntOrNullFlow(KeyAnswerPageStyle)
        .mappedStateIn(
            coroutineScope,
            started = SharingStarted.Lazily,
            defaultValue = PageStyle.Horizontal
        ) {
            PageStyle.entries[it]
        }

    fun setAnswerPageStyle(value: PageStyle) =
        default.putInt(KeyAnswerPageStyle, value.ordinal)

    val showAccuracy =
        default.getBooleanStateFlow(
            coroutineScope,
            key = KeyShowAccuracy,
            defaultValue = false,
            sharingStarted = SharingStarted.Lazily
        )

    fun setShowAccuracy(value: Boolean) =
        default.putBoolean(KeyShowAccuracy, value)

    val showNextQuizAutomatically = default.getBooleanStateFlow(
        coroutineScope,
        key = KeyQuizAutoplay,
        defaultValue = true,
        sharingStarted = SharingStarted.Lazily
    )
    val feiModelIndex = default.getIntStateFlow(
        coroutineScope,
        key = KeyFeiModelId,
        defaultValue = 0,
        sharingStarted = SharingStarted.Lazily
    )

    fun setFeiModelIndex(value: Int) =
        default.putInt(KeyFeiModelId, value)

    val feiCompatibilityMode = default.getBooleanStateFlow(
        coroutineScope,
        key = KeyFeiCompatibility,
        defaultValue = false,
        sharingStarted = SharingStarted.Lazily
    )

    fun setFeiCompatibilityMode(value: Boolean) =
        default.putBoolean(KeyFeiCompatibility, value)

    val communityUseCustomServer = default.getBooleanStateFlow(
        coroutineScope,
        key = KeyCommunityUseCustomServer,
        defaultValue = false,
        sharingStarted = SharingStarted.Lazily
    )

    fun setCommunityUseCustomServer(value: Boolean) =
        default.putBoolean(KeyCommunityUseCustomServer, value)

    val communityServerUrl = default.getStringOrNullStateFlow(
        coroutineScope,
        key = KeyCommunityServerUrl,
        sharingStarted = SharingStarted.Lazily
    )

    fun setCommunityServerUrl(value: String?) {
        if (value == null) {
            default.remove(KeyCommunityServerUrl)
        } else {
            default.putString(KeyCommunityServerUrl, value)
        }
    }

    val clientName = default.getStringStateFlow(
        coroutineScope,
        key = KeyClientName,
        defaultValue = getPlatform().deviceName,
        sharingStarted = SharingStarted.Lazily
    )

    fun getCommunityIdentity(serverUrl: String) =
        HybridSettingsCommunityIdentity(serverUrl, coroutineScope, default, secure)
}

@OptIn(ExperimentalSettingsApi::class)
class HybridSettingsCommunityIdentity(
    serverUrl: String,
    coroutineScope: CoroutineScope,
    private val insecure: ObservableSettings,
    private val secure: ObservableSettings = insecure,
) : CommunityIdentity {
    private val keyPrefix =
        serverUrl.encodeBase64()

    init {
        val legacyKey = "" // caused by a bug
        val legacyToken = secure.getStringOrNull("${legacyKey}_token")
        if (legacyToken != null) {
            // migrate
            secure.putString("${keyPrefix}_token", legacyToken)
            secure.remove("${legacyKey}_token")
        }
    }

    override val authToken =
        secure.getStringOrNullFlow("${keyPrefix}_token")
            .mappedStateIn(
                coroutineScope,
                started = SharingStarted.Eagerly,
                defaultValue = null
            ) { AuthorizationToken(it) }

    override fun clear() {
        secure.remove("${keyPrefix}_token")
    }

    override fun setAuthToken(value: AuthorizationToken) {
        secure.putString("${keyPrefix}_token", value.toString())
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

private inline fun <T, O> Flow<O?>.mappedStateIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.Lazily,
    defaultValue: T,
    crossinline mapper: (O) -> T,
) = map { it?.let(mapper) ?: defaultValue }
    .stateIn(scope, started, defaultValue)