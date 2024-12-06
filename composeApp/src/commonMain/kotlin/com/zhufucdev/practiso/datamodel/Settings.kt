package com.zhufucdev.practiso.datamodel

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val KeyAnswerPageStyle = "answer_page_style"
private const val KeyShowAnswerImmediately = "show_answer_immediately"
private const val KeyQuizAutoplay = "quiz_autoplay"
private const val KeyDbVersion = "db_version"

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
    val showAnswerImmediately =
        MutableStateFlow(settings.getBoolean(KeyShowAnswerImmediately, false))
    val showNextQuizAutomatically = MutableStateFlow(settings.getBoolean(KeyQuizAutoplay, false))
    val databaseVersion = MutableStateFlow(settings.getLongOrNull(KeyDbVersion))

    init {
        coroutineScope.launch {
            answerPageStyle.collectLatest {
                settings.putInt(KeyAnswerPageStyle, it.ordinal)
            }
        }
        coroutineScope.launch {
            showAnswerImmediately.collectLatest {
                settings.putBoolean(KeyShowAnswerImmediately, it)
            }
        }
        coroutineScope.launch {
            showNextQuizAutomatically.collectLatest {
                settings.putBoolean(KeyQuizAutoplay, it)
            }
        }
    }
}