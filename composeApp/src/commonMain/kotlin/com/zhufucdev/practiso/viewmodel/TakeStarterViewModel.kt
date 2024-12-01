package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.zhufucdev.practiso.composable.FlipCardState
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.platform.randomUUID
import com.zhufucdev.practiso.protoBufStateListSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@OptIn(SavedStateHandleSaveableApi::class)
class TakeStarterViewModel(
    val db: AppDatabase,
    val option: PractisoOption.Session,
    private val coroutineScope: CoroutineScope,
    state: SavedStateHandle,
) : ViewModel() {
    val takeStats by lazy {
        MutableStateFlow<List<TakeStat>?>(null).apply {
            viewModelScope.launch {
                db.sessionQueries.getTakeStatsBySession(option.session.id)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .collect(this@apply)
            }
        }
    }

    val flipCardState = FlipCardState()

    @Serializable
    data class Timer(val duration: Duration, val id: String = randomUUID())

    val timers by state.saveable(saver = protoBufStateListSaver()) {
        mutableStateListOf<Timer>()
    }

    var currentTakeId by state.saveable { mutableLongStateOf(-1) }
        private set

    data class Events(
        val create: Channel<Unit> = Channel(),
        val start: Channel<Unit> = Channel(),
        val tapTake: Channel<Long> = Channel(),
        val flip: Channel<Int> = Channel()
    )

    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.create.onReceive {
                        val takeId = db.transactionWithResult {
                            db.sessionQueries.updateSessionAccessTime(
                                Clock.System.now(),
                                option.session.id
                            )
                            db.sessionQueries.insertTake(
                                sessionId = option.session.id,
                                creationTimeISO = Clock.System.now(),
                            )
                            db.quizQueries.lastInsertRowId().executeAsOne()
                        }

                        db.transaction {
                            timers.forEach { d ->
                                db.sessionQueries.associateTimerWithTake(
                                    takeId,
                                    durationSeconds = d.duration.inWholeMilliseconds / 1000.0
                                )
                            }
                        }

                        timers.clear()
                    }

                    event.flip.onReceive {
                        coroutineScope.launch {
                            flipCardState.flip(it)
                        }
                    }

                    event.start.onReceive {

                    }

                    event.tapTake.onReceive {
                        if (currentTakeId == it) {
                            currentTakeId = -1
                        } else {
                            currentTakeId = it
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun factory(option: PractisoOption.Session, coroutineScope: CoroutineScope) =
            viewModelFactory {
                initializer {
                    TakeStarterViewModel(
                        Database.app,
                        option,
                        coroutineScope,
                        createPlatformSavedStateHandle()
                    )
                }
            }
    }
}