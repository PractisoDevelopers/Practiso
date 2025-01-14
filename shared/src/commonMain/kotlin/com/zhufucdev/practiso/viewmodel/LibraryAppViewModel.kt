package com.zhufucdev.practiso.viewmodel

import androidx.core.bundle.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.createSession
import com.zhufucdev.practiso.datamodel.createTake
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.toOptionFlow
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class LibraryAppViewModel(private val db: AppDatabase, state: SavedStateHandle) : ViewModel() {
    val templates by lazy {
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    val quiz: Flow<List<PractisoOption.Quiz>> by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
    }

    val dimensions by lazy {
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .toOptionFlow(db.quizQueries)
    }

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
        override fun get(bundle: Bundle, key: String): Revealable? =
            bundle.getString(key)?.let(::parseValue)

        override fun parseValue(value: String): Revealable =
            value.split("_").let {
                Revealable(
                    type = RevealableType.valueOf(it[0]),
                    id = it[1].toLong()
                )
            }

        override fun put(
            bundle: Bundle,
            key: String,
            value: Revealable,
        ) {
            bundle.putString(key, "${value.type.name}_${value.id}")
        }
    }

    object RevealableTypeNavType : NavType<RevealableType>(isNullableAllowed = false) {
        override fun get(
            bundle: Bundle,
            key: String,
        ): RevealableType? = bundle.getString(key)?.let(RevealableType::valueOf)

        override fun parseValue(value: String): RevealableType = RevealableType.valueOf(value)

        override fun put(
            bundle: Bundle,
            key: String,
            value: RevealableType,
        ) {
            bundle.putString(key, value.name)
        }
    }

    val revealing: StateFlow<Revealable?> get() = _revealing
    val caps: StateFlow<Caps> get() = _caps

    data class Events(
        val removeQuiz: Channel<Long> = Channel(),
        val removeDimensionWithQuizzes: Channel<Long> = Channel(),
        val removeDimensionKeepQuizzes: Channel<Long> = Channel(),
        val reveal: Channel<Revealable> = Channel(),
        val newTakeFromDimension: Channel<Long> = Channel(),
        val updateCaps: Channel<Caps> = Channel(),
    )

    val event = Events()

    private suspend fun removeQuizWithResources(id: Long) {
        val quizFrames = db.quizQueries
            .getQuizFrames(db.quizQueries.getQuizById(id))
            .firstOrNull()
        if (quizFrames.isNullOrEmpty()) {
            return
        }

        withContext(Dispatchers.IO) {
            val platform = getPlatform()
            quizFrames.first()
                .frames
                .map(PrioritizedFrame::frame)
                .resources()
                .map { (name) ->
                    async {
                        platform.filesystem.delete(
                            platform.resourcePath.resolve(
                                name
                            )
                        )
                    }
                }
                .awaitAll()
        }

        db.transaction {
            db.quizQueries.removeQuiz(id)
        }
    }


    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.removeQuiz.onReceive {
                        removeQuizWithResources(it)
                    }

                    event.removeDimensionKeepQuizzes.onReceive {
                        db.transaction {
                            db.dimensionQueries.removeDimension(it)
                        }
                    }

                    event.removeDimensionWithQuizzes.onReceive {
                        db.transaction {
                            db.quizQueries.removeQuizWithinDimension(it)
                            db.dimensionQueries.removeDimension(it)
                        }
                    }

                    event.reveal.onReceive {
                        _revealing.emit(it)
                    }

                    event.newTakeFromDimension.onReceive {
                        val dimension =
                            db.dimensionQueries.getDimensionById(it).executeAsOneOrNull()
                        if (dimension == null) {
                            return@onReceive
                        }
                        val sessionId =
                            createSession(dimension.name, Selection(dimensionIds = setOf(it)), db)
                        val takeId = createTake(sessionId, emptyList(), db)
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer), options = listOf(
                                NavigationOption.OpenTake(takeId)
                            )
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