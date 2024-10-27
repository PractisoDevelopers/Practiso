package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.createPlatformSavedStateHandle
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.getFramedQuizzes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

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

    // TODO: recommend based on error rates, quiz legitimacy, etc
    val smartRecommendations by lazy {
        db.quizQueries.getFramedQuizzes(db.quizQueries.getAllQuiz())
            .toSessionStarterOptionFlow()
    }

    val recentRecommendations by lazy {
        db.quizQueries.getFramedQuizzes(db.quizQueries.getRecentQuiz(5))
            .toSessionStarterOptionFlow()
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