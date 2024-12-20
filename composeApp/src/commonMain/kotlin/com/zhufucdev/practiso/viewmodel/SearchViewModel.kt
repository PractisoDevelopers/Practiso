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
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.protobufMutableStateFlowSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex

@OptIn(SavedStateHandleSaveableApi::class)
class SearchViewModel(state: SavedStateHandle, private val db: AppDatabase) : ViewModel() {
    val active by state.saveable(saver = protobufMutableStateFlowSaver()) { MutableStateFlow(false) }
    val query by state.saveable(saver = protobufMutableStateFlowSaver()) { MutableStateFlow("") }
    val searching = MutableStateFlow(false)
    val result = MutableStateFlow(emptyList<PractisoOption>()).apply {
        viewModelScope.launch {
            query.collectLatest { query ->
                if (query.isEmpty()) {
                    emit(emptyList())
                    searching.emit(false)
                    return@collectLatest
                }
                searching.emit(true)
                coroutineScope {
                    val options = mutableListOf<PractisoOption>()
                    val mutex = Mutex()
                    launch(Dispatchers.IO) {
                        val quizFrames = db.quizQueries
                            .getQuizFrames(db.quizQueries.getAllQuiz())
                            .toOptionFlow()
                            .first()
                            .filter {
                                it.quiz.name?.contains(query) == true
                                        || it.preview?.contains(query) == true
                            }
                        mutex.lock()
                        options.addAll(quizFrames)
                        emit(options.toList())
                        mutex.unlock()
                    }

                    launch(Dispatchers.IO) {
                        val dimensions = db.dimensionQueries
                            .getAllDimensions()
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .toOptionFlow(db.quizQueries)
                            .first()
                            .filter { query in it.dimension.name }
                        mutex.lock()
                        options.addAll(dimensions)
                        emit(options.toList())
                        mutex.unlock()
                    }
                }
                searching.emit(false)
            }
        }
    }

    data class Events(
        val open: Channel<Unit> = Channel(),
        val close: Channel<Unit> = Channel(),
        val updateQuery: Channel<String> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.open.onReceive {
                        active.emit(true)
                    }

                    event.close.onReceive {
                        active.emit(false)
                    }

                    event.updateQuery.onReceive {
                        query.emit(it)
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SearchViewModel(createPlatformSavedStateHandle(), Database.app)
            }
        }
    }
}
