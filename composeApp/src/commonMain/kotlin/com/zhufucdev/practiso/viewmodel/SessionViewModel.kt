package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateOf
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
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class SessionViewModel(private val db: AppDatabase, private val state: SavedStateHandle) :
    ViewModel() {
    val sessions: Flow<List<Session>> =
        db.sessionQueries.getAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)

    val recentTakeStats: Flow<List<TakeStat>> by lazy {
        db.sessionQueries.getRecentTakeStats(5)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    var useRecommendations by state.saveable { mutableStateOf(true) }
        private set

    data class Events(
        val toggleRecommendations: Channel<Boolean> = Channel()
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.toggleRecommendations.onReceive {
                        useRecommendations = it
                    }
                }
            }
        }
    }

    // TODO: recommend based on error rates, quiz legitimacy, etc
    val smartRecommendations by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
    }

    val recentRecommendations by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getRecentQuiz(5))
            .toOptionFlow()
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