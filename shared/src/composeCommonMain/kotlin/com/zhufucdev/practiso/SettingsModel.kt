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
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
val AppSettings = with(CoroutineScope(newSingleThreadContext("AppSettings"))) {
    val model = SettingsModel(getDefaultSettingsFactory().create(), this)
    launch {
        model.feiModelIndex.collectLatest { modelIdx ->
            Database.fei.setFeiModel(KnownModels[modelIdx])
        }
    }
    model
}
