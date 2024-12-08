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
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class SessionViewModel(private val db: AppDatabase, state: SavedStateHandle) :
    ViewModel() {
    val sessions by lazy {
        MutableStateFlow<List<PractisoOption.Session>?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                db.sessionQueries.getAllSessions()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .toOptionFlow(db.sessionQueries)
                    .collect(this@apply)
            }
        }
    }

    val recentTakeStats by lazy {
        MutableStateFlow<List<TakeStat>?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                db.sessionQueries.getRecentTakeStats(5)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { it.filterNot { stat -> stat.hidden == 1L } }
                    .collect(this@apply)
            }
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    var useRecommendations by state.saveable { mutableStateOf(true) }
        private set


    data class Events(
        val toggleRecommendations: Channel<Boolean> = Channel(),
        val deleteSession: Channel<Long> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.toggleRecommendations.onReceive {
                        useRecommendations = it
                    }

                    event.deleteSession.onReceive {
                        db.transaction {
                            db.sessionQueries.removeSession(it)
                        }
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