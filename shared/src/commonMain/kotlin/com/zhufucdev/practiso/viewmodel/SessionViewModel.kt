package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.SessionCreator
import com.zhufucdev.practiso.datamodel.SessionOption
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toOptionFlow
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.LibraryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.n_questions_in_dimension
import resources.new_question_para
import resources.x_and_n_more_para

class SessionViewModel(val db: AppDatabase, state: SavedStateHandle) :
    ViewModel() {
    private val libraryService = LibraryService()

    val sessions by lazy {
        MutableStateFlow<List<SessionOption>?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                libraryService.getSessions().collect(this@apply)
            }
        }
    }

    val recentTakeStats by lazy {
        MutableStateFlow<List<TakeStat>?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                libraryService.getRecentTakes().collect(this@apply)
            }
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    val useRecommendations by state.saveable(saver = protobufMutableStateFlowSaver()) {
        MutableStateFlow(true)
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    val currentCreatorIndex by state.saveable(saver = protobufMutableStateFlowSaver()) {
        MutableStateFlow(
            -1
        )
    }

    data class Events(
        val toggleRecommendations: Channel<Boolean> = Channel(),
        val toggleCreator: Channel<Int> = Channel(),
        val deleteSession: Channel<Long> = Channel(),
        val startTake: Channel<Long> = Channel(),
        val toggleTakePin: Channel<Long> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.toggleRecommendations.onReceive {
                        currentCreatorIndex.emit(-1)
                        useRecommendations.emit(it)
                    }

                    event.toggleCreator.onReceive {
                        if (it == currentCreatorIndex.value) {
                            currentCreatorIndex.emit(-1)
                        } else {
                            currentCreatorIndex.emit(it)
                        }
                    }

                    event.deleteSession.onReceive {
                        db.transaction {
                            db.sessionQueries.removeSession(it)
                        }
                    }

                    event.toggleTakePin.onReceive {
                        db.transaction {
                            val pinned = db.sessionQueries.getTakePinnedById(it).executeAsOne()
                            db.sessionQueries.updateTakePin(pinned xor 1, it)
                        }
                    }

                    event.startTake.onReceive {
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer),
                            options = listOf(NavigationOption.OpenTake(it))
                        )
                    }
                }
            }
        }
    }

    // TODO: recommend based on error rates, quiz legitimacy, etc
    val smartRecommendations by lazy {
        recentRecommendations
    }

    val recentRecommendations = MutableStateFlow<List<SessionCreator>?>(null).apply {
        viewModelScope.launch(Dispatchers.IO) {
            db.quizQueries.getQuizFrames(db.quizQueries.getRecentQuiz())
                .toOptionFlow()
                .collectLatest { quizzes ->
                    db.dimensionQueries.getRecentDimensions(5)
                        .asFlow()
                        .mapToList(Dispatchers.IO)
                        .toOptionFlow(db.quizQueries)
                        .collectLatest { dimensions ->
                            val emission = buildList {
                                if (quizzes.isNotEmpty()) {
                                    val firstName = quizzes.first().quiz.name
                                        ?: getString(Res.string.new_question_para)
                                    add(
                                        SessionCreator.ViaSelection(
                                            selection = Selection(
                                                quizIds = quizzes.map(PractisoOption::id).toSet()
                                            ),
                                            type = SessionCreator.ViaSelection.Type.RecentlyCreated,
                                            preview = if (quizzes.size > 1) {
                                                getString(
                                                    Res.string.x_and_n_more_para,
                                                    firstName,
                                                    quizzes.size - 1
                                                )
                                            } else {
                                                firstName
                                            }
                                        )
                                    )
                                }

                                dimensions.forEach {
                                    add(
                                        SessionCreator.ViaSelection(
                                            selection = Selection(
                                                dimensionIds = setOf(it.dimension.id)
                                            ),
                                            type = SessionCreator.ViaSelection.Type.RecentlyCreated,
                                            preview = getPluralString(
                                                Res.plurals.n_questions_in_dimension,
                                                it.quizCount,
                                                it.quizCount,
                                                it.dimension.name
                                            )
                                        )
                                    )
                                }
                            }
                            emit(emission)
                        }
                }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            val db = Database.app
            initializer {
                SessionViewModel(db, createPlatformSavedStateHandle())
            }
        }
    }
}