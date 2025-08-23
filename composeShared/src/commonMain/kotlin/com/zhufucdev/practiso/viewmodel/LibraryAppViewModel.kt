package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.CategorizeService
import com.zhufucdev.practiso.service.CreateService
import com.zhufucdev.practiso.service.LibraryService
import com.zhufucdev.practiso.service.RemoveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

class LibraryAppViewModel(private val db: AppDatabase, state: SavedStateHandle) : ViewModel() {
    private val libraryService = LibraryService(db)
    private val createService = CreateService(db)
    private val categoryService = CategorizeService(db)

    val templates =
        libraryService.getTemplates()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = null)

    val quiz =
        libraryService.getQuizzes()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = null)

    val dimensions =
        libraryService.getDimensions()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, initialValue = null)

    private val removeService = RemoveService(db)

    @Serializable
    data class Caps(val template: Int = 5, val quiz: Int = 5, val dimension: Int = 5)

    @OptIn(SavedStateHandleSaveableApi::class)
    private val _caps by state.saveable(saver = protobufMutableStateFlowSaver<Caps>()) {
        MutableStateFlow(Caps())
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    private val _revealing by state.saveable(saver = protobufMutableStateFlowSaver()) {
        MutableStateFlow<Revealable?>(null)
    }

    @Serializable
    data class Revealable(val id: Long, val type: RevealableType)

    @Serializable
    enum class RevealableType {
        Dimension, Quiz
    }

    object RevealableNavType : NavType<Revealable>(isNullableAllowed = false) {
        override fun get(bundle: SavedState, key: String): Revealable? =
            bundle.read { getStringOrNull(key)?.let(::parseValue) }

        override fun parseValue(value: String): Revealable =
            value.split("_").let {
                Revealable(
                    type = RevealableType.valueOf(it[0]),
                    id = it[1].toLong()
                )
            }

        override fun put(
            bundle: SavedState,
            key: String,
            value: Revealable,
        ) {
            bundle.write {
                putString(key, "${value.type.name}_${value.id}")
            }
        }
    }

    object RevealableTypeNavType : NavType<RevealableType>(isNullableAllowed = false) {
        override fun get(
            bundle: SavedState,
            key: String,
        ): RevealableType? = bundle.read { getStringOrNull(key)?.let(RevealableType::valueOf) }

        override fun parseValue(value: String): RevealableType = RevealableType.valueOf(value)

        override fun put(
            bundle: SavedState,
            key: String,
            value: RevealableType,
        ) {
            bundle.write {
                putString(key, value.name)
            }
        }
    }

    val revealing: StateFlow<Revealable?> get() = _revealing
    val caps: StateFlow<Caps> get() = _caps

    data class Events(
        val removeQuiz: Channel<Long> = Channel(),
        val removeDimensionWithQuizzes: Channel<Long> = Channel(),
        val removeDimensionKeepQuizzes: Channel<Long> = Channel(),
        val reveal: Channel<Revealable> = Channel(),
        val removeReveal: Channel<Unit> = Channel(),
        val newTakeFromDimension: Channel<Long> = Channel(),
        val updateCaps: Channel<Caps> = Channel(),
        val newDimension: Channel<String> = Channel()
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.removeQuiz.onReceive(
                        removeService::removeQuizWithResources
                    )

                    event.removeDimensionKeepQuizzes.onReceive(
                        removeService::removeDimensionKeepQuizzes
                    )

                    event.removeDimensionWithQuizzes.onReceive(
                        removeService::removeDimensionWithQuizzes
                    )

                    event.reveal.onReceive {
                        _revealing.emit(it)
                    }

                    event.removeReveal.onReceive {
                        _revealing.emit(null)
                    }

                    event.newTakeFromDimension.onReceive {
                        val dimension =
                            db.dimensionQueries.getDimensionById(it).executeAsOneOrNull()
                        if (dimension == null) {
                            return@onReceive
                        }
                        val sessionId = createService.createSession(
                            dimension.name,
                            Selection(dimensionIds = setOf(it)),
                        )
                        val takeId = createService.createTake(sessionId, emptyList())
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer),
                            NavigationOption.OpenTake(takeId)
                        )
                    }

                    event.updateCaps.onReceive {
                        _caps.emit(
                            Caps(
                                template = maxOf(_caps.value.template, it.template),
                                quiz = maxOf(_caps.value.quiz, it.quiz),
                                dimension = maxOf(_caps.value.dimension, it.dimension)
                            )
                        )
                    }

                    event.newDimension.onReceive {
                        categoryService.createDimension(it)
                    }
                }
            }
        }
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    LibraryAppViewModel(Database.app, createPlatformSavedStateHandle())
                }
            }
    }
}