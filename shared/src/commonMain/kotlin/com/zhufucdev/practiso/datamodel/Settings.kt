package com.zhufucdev.practiso.datamodel

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val KeyAnswerPageStyle = "answer_page_style"
private const val KeyShowAccuracy = "show_accuracy"
private const val KeyQuizAutoplay = "quiz_autoplay"
private const val KeyFeiModelId = "fei_model_id"

enum class PageStyle {
    Horizontal,
    Vertical,
    Column
}

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
        }
    }
}