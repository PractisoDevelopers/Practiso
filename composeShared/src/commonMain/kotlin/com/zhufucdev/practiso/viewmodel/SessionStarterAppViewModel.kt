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
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.toQuizOptionFlow
import com.zhufucdev.practiso.helper.protobufSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

@OptIn(SavedStateHandleSaveableApi::class)
class SessionStarterAppViewModel(private val db: AppDatabase, state: SavedStateHandle) :
    ViewModel() {
    var newSessionName by state.saveable { mutableStateOf("") }
    var selection by state.saveable(stateSaver = protobufSaver()) { mutableStateOf(Selection()) }
        private set

    data class Events(
        val selectQuiz: Channel<Long> = Channel(),
        val deselectQuiz: Channel<Long> = Channel(),
        val selectCategory: Channel<Long> = Channel(),
        val deselectCategory: Channel<Long> = Channel(),
        val createSession: Channel<String> = Channel(),
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.selectQuiz.onReceive {
                        selection = selection.copy(quizIds = selection.quizIds + it)
                    }

                    event.deselectQuiz.onReceive {
                        selection = selection.copy(quizIds = selection.quizIds - it)
                    }

                    event.selectCategory.onReceive {
                        selection = selection.copy(dimensionIds = selection.dimensionIds + it)
                    }

                    event.deselectCategory.onReceive {
                        selection = selection.copy(dimensionIds = selection.dimensionIds - it)
                    }

                    event.createSession.onReceive { name ->
                        withContext(Dispatchers.IO) {
                            val sessionId = db.transactionWithResult {
                                db.sessionQueries.insertSesion(name)
                                db.quizQueries.lastInsertRowId().executeAsOne()
                            }

                            val quizzes =
                                items.value!!.mapNotNull { item -> item.quiz.id.takeIf { item.dimensions.any { dim -> dim.id in selection.dimensionIds } } }
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

    val items =
        db.quizQueries
            .getQuizFrames(db.quizQueries.getAllQuiz())
            .toQuizOptionFlow()
            .map { options ->
                coroutineScope {
                    options.map { option ->
                        async {
                            Item(
                                quiz = option,
                                dimensions =
                                    db.dimensionQueries.getDimensionByQuizId(option.quiz.id) { id, name, _ ->
                                        Dimension(id, name)
                                    }.awaitAsList()
                            )
                        }
                    }.awaitAll()
                }
            }
            .stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    data class Item(val quiz: QuizOption, val dimensions: List<Dimension>)

    fun isItemSelected(item: Item): Boolean {
        return item.quiz.id in selection.quizIds || item.dimensions.any { it.id in selection.dimensionIds }
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
