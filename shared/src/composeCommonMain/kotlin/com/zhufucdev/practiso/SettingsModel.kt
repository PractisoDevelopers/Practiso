package com.zhufucdev.practiso

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

private const val KeyAnswerPageStyle = "answer_page_style"
private const val KeyShowAccuracy = "show_accuracy"
private const val KeyQuizAutoplay = "quiz_autoplay"
private const val KeyFeiModelId = "fei_model_id"
private const val KeyFeiCompatibility = "fei_compatibility"
private const val KeyCommunityServerUrl = "community_server_url"
private const val KeyCommunityUseCustomServer = "community_use_custom_ser"

class SettingsModel(private val settings: Settings, val coroutineScope: CoroutineScope) {
    val answerPageStyle = MutableStateFlow(
        PageStyle.entries[settings.getInt(
            KeyAnswerPageStyle,
            PageStyle.Horizontal.ordinal
        )]
    )
    val showAccuracy =
        MutableStateFlow(settings.getBoolean(KeyShowAccuracy, false))
    val showNextQuizAutomatically = MutableStateFlow(settings.getBoolean(KeyQuizAutoplay, false))
    val feiModelIndex = MutableStateFlow(settings.getInt(KeyFeiModelId, defaultValue = 0))
    val feiCompatibilityMode =
        MutableStateFlow(settings.getBoolean(KeyFeiCompatibility, defaultValue = false))
    val communityUseCustomServer =
        MutableStateFlow(settings.getBoolean(KeyCommunityUseCustomServer, defaultValue = false))
    val communityServerUrl =
        MutableStateFlow(settings.getStringOrNull(KeyCommunityServerUrl))

    init {
        with(coroutineScope) {
            launch {
                answerPageStyle.collectLatest {
                    settings.putInt(KeyAnswerPageStyle, it.ordinal)
                }
            }
            launch {
                showAccuracy.collectLatest {
                    settings.putBoolean(KeyShowAccuracy, it)
                }
            }
            launch {
                showNextQuizAutomatically.collectLatest {
                    settings.putBoolean(KeyQuizAutoplay, it)
                }
            }
            launch {
                feiModelIndex.collectLatest {
                    settings.putInt(KeyFeiModelId, it)
                }
            }
            launch {
                feiCompatibilityMode.collectLatest {
                    settings.putBoolean(KeyFeiCompatibility, it)
                }
            }
            launch {
                communityServerUrl.collectLatest {
                    if (it == null) {
                        settings.remove(KeyCommunityServerUrl)
                    } else {
                        settings.putString(KeyCommunityServerUrl, it)
                    }
                }
            }
            launch {
                communityUseCustomServer.collectLatest {
                    settings.putBoolean(KeyCommunityUseCustomServer, it)
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
object AppSettingsScope : CoroutineScope by CoroutineScope(newSingleThreadContext("AppSettings"))

val AppSettings = with(AppSettingsScope) {
    val model = SettingsModel(getDefaultSettingsFactory().create(), this)
    launch {
        model.feiModelIndex.collectLatest { modelIdx ->
            Database.fei.setFeiModel(KnownModels[modelIdx])
        }
    }
    model
}
