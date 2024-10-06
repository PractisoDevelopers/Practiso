package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class SessionViewModel(private val db: AppDatabase, private val state: SavedStateHandle) : ViewModel() {
    private val _sessions = mutableStateListOf<Session>()
    val sessions: List<Session> get() = _sessions

    init {
        viewModelScope.launch {
            db.sessionQueries.getAllSessions()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .collect {
                    _sessions.clear()
                    _sessions.addAll(it)
                }
        }
    }
}