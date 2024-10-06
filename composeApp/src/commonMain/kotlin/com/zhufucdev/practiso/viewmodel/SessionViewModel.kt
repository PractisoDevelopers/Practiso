package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.TakeStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class SessionViewModel(private val db: AppDatabase, private val state: SavedStateHandle) : ViewModel() {
    val sessions: Flow<List<Session>> =
        db.sessionQueries.getAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)

    val recentTakeStats: Flow<List<TakeStat>> by lazy {
        db.sessionQueries.getRecentTakeStats(5)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }
}