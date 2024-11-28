package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.concat
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.datamodel.getQuizFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@OptIn(SavedStateHandleSaveableApi::class)
class SessionStarterAppViewModel(private val db: AppDatabase, state: SavedStateHandle) :
    ViewModel() {
    var currentItemIds by state.saveable { mutableStateOf(emptySet<Long>()) }
        private set

    @Serializable
    data class Selection(
        val quizIds: Set<Long> = emptySet(),
        val dimensionIds: Set<Long> = emptySet(),
    )

    var selection by state.saveable { mutableStateOf(Selection()) }
        private set

    data class Events(
        val addCurrentItem: Channel<Long> = Channel(),
        val removeCurrentItem: Channel<Long> = Channel(),
        val selectQuiz: Channel<Long> = Channel(),
        val deselectQuiz: Channel<Long> = Channel(),
        val createSession: Channel<String> = Channel()
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.addCurrentItem.onReceive {
                        currentItemIds += it
                        Unit
                    }

                    event.removeCurrentItem.onReceive {
                        currentItemIds -= it
                        Unit
                    }

                    event.selectQuiz.onReceive {
                        selection = selection.copy(quizIds = selection.quizIds + it)
                        Unit
                    }

                    event.deselectQuiz.onReceive {
                        selection = selection.copy(quizIds = selection.quizIds - it)
                        Unit
                    }

                    event.createSession.onReceive { name ->
                        withContext(Dispatchers.IO) {
                            val sessionId = db.transactionWithResult {
                                db.sessionQueries.insertSession(name, Clock.System.now())
                                db.quizQueries.lastInsertRowId().executeAsOne()
                            }

                            val quizzes = items.value!!.filter { it.id in selection.dimensionIds }
                                .flatMap { item -> item.quizzes.map { it.quiz.id } }
                                .toSet() + selection.quizIds
                            db.transaction {
                                quizzes.forEach {
                                    db.sessionQueries.assoicateQuizWithSession(it, sessionId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    interface Item {
        val quizzes: List<PractisoOption.Quiz>
        val id: Long

        data class Categorized(
            val dimension: Dimension,
            override val quizzes: List<PractisoOption.Quiz>,
        ) : Item {
            override val id: Long
                get() = dimension.id
        }

        data class Stranded(
            override val quizzes: List<PractisoOption.Quiz>,
        ) : Item {
            override val id: Long
                get() = 0
        }
    }

    val items by lazy {
        val categorizedFlow: Flow<List<Item>> = db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map {
                coroutineScope {
                    it.map { dimension ->
                        async {
                            Item.Categorized(
                                dimension = dimension,
                                quizzes = db.quizQueries
                                    .getQuizFrames(db.quizQueries.getQuizByDimension(dimension.id))
                                    .toOptionFlow()
                                    .last()
                            )
                        }
                    }.awaitAll()
                }
            }
        val stranded: Flow<List<Item>> =
            db.quizQueries.getQuizFrames(db.quizQueries.getStrandedQuiz())
                .toOptionFlow()
                .map {
                    if (it.isNotEmpty()) {
                        listOf(Item.Stranded(it))
                    } else {
                        emptyList()
                    }
                }
        val result = MutableStateFlow<List<Item>?>(null)
        viewModelScope.launch {
            categorizedFlow.concat(stranded).collect(result)
        }
        result
    }

    companion object {
        val Factory = viewModelFactory {
            val db = Database.app
            initializer {
                SessionStarterAppViewModel(db, createSavedStateHandle())
            }
        }
    }
}
