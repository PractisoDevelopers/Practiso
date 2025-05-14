package com.zhufucdev.practiso.viewmodel

import androidx.core.bundle.Bundle
import androidx.core.bundle.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.RemoveService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

class QuizSectionEditVM(
    private val db: AppDatabase = Database.app,
    private val removeService: RemoveService = RemoveService(db),
    state: SavedStateHandle,
) : SectionEditViewModel<QuizOption>(state) {
    data class Event(
        val removeSection: Channel<Unit> = Channel(),
    )

    val events = Event()

    fun loadStartpoint(value: Startpoint) {
        _selection.add(value.quizId)
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    events.removeSection.onReceive {
                        removeService.removeQuizWithResources(selection)
                        _selection.clear()
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                QuizSectionEditVM(state = createPlatformSavedStateHandle())
            }
        }
    }

    @Serializable
    data class Startpoint(val quizId: Long, val topItemIndex: Int)

    object StartpointNavType : NavType<Startpoint>(false) {
        private const val QuizIdKey = "quiz_id"
        private const val TopIndexIdxKey = "top_item_idx"

        override fun get(
            bundle: Bundle,
            key: String,
        ): Startpoint? =
            bundle.getBundle(key)?.let {
                Startpoint(it.getLong(QuizIdKey), it.getInt(TopIndexIdxKey))
            }

        override fun parseValue(value: String): Startpoint {
            throw UnsupportedOperationException()
        }

        override fun put(
            bundle: Bundle,
            key: String,
            value: Startpoint,
        ) {
            bundle.putBundle(
                key, bundleOf(
                    QuizIdKey to value.quizId,
                    TopIndexIdxKey to value.topItemIndex
                )
            )
        }
    }
}