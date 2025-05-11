package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.SessionOption
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.LibraryService
import com.zhufucdev.practiso.service.RecommendationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class SessionViewModel(val db: AppDatabase, state: SavedStateHandle) :
    ViewModel() {
    private val libraryService = LibraryService()
    private val recommendationService = RecommendationService()

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
        val renameSession: Channel<Pair<Long, String>> = Channel(),
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

                    event.renameSession.onReceive {
                        val (id, name) = it
                        db.transaction {
                            db.sessionQueries.renameSession(name.trim(), id)
                        }
                    }
                }
            }
        }
    }

    val smartRecommendations by lazy {
        recommendationService.getSmartRecommendations()
    }

    val recentRecommendations by lazy {
        recommendationService.getRecentRecommendations()
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