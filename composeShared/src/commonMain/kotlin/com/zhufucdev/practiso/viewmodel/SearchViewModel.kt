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
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toDimensionOptionFlow
import com.zhufucdev.practiso.datamodel.toQuizOptionFlow
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex

@OptIn(SavedStateHandleSaveableApi::class)
class SearchViewModel(state: SavedStateHandle, private val db: AppDatabase) : ViewModel() {
    val active by state.saveable(saver = protobufMutableStateFlowSaver()) { MutableStateFlow(false) }
    val expanded by state.saveable(saver = protobufMutableStateFlowSaver()) { MutableStateFlow(false) }
    val query by state.saveable(saver = protobufMutableStateFlowSaver()) { MutableStateFlow("") }
    val searching = MutableStateFlow(false)
    @OptIn(ExperimentalCoroutinesApi::class)
    val result =
        query.flatMapMerge { query ->
            if (query.isEmpty()) {
                searching.emit(false)
                return@flatMapMerge flowOf(emptyList())
            }
            val tokens = query.split(Regex(" +"))

            searching.emit(true)
            channelFlow {
                coroutineScope {
                    val options = mutableListOf<PractisoOption>()
                    val mutex = Mutex()
                    launch {
                        val quizFrames = db.quizQueries
                            .getQuizFrames(db.quizQueries.getAllQuiz())
                            .toQuizOptionFlow()
                            .first()
                            .filter {
                                it.quiz.name?.let { name -> tokens.any { t -> t in name } } == true
                                        || it.preview?.let { preview -> tokens.any { t -> t in preview } } == true
                            }

                        mutex.lock()
                        options.addAll(quizFrames)
                        send(options.toList())
                        mutex.unlock()
                    }

                    launch {
                        val dimensions = db.dimensionQueries
                            .getAllDimensions()
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .toDimensionOptionFlow(db.quizQueries)
                            .first()
                            .filter { tokens.any { t -> t in it.dimension.name } }
                        mutex.lock()
                        options.addAll(dimensions)
                        send(options.toList())
                        mutex.unlock()
                    }
                }
                searching.emit(false)
            }
        }

    data class Events(
        val open: Channel<Unit> = Channel(),
        val close: Channel<Unit> = Channel(),
        val hide: Channel<Unit> = Channel(),
        val updateQuery: Channel<String> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.open.onReceive {
                        expanded.emit(true)
                        active.emit(true)
                    }

                    event.close.onReceive {
                        active.emit(false)
                        expanded.emit(false)
                    }

                    event.hide.onReceive {
                        expanded.emit(false)
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
