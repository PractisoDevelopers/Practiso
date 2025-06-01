package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.FlipCardState
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.SessionOption
import com.zhufucdev.practiso.helper.protoBufStateListSaver
import com.zhufucdev.practiso.helper.protobufSaver
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.platform.randomUUID
import com.zhufucdev.practiso.service.CreateService
import com.zhufucdev.practiso.service.LibraryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(SavedStateHandleSaveableApi::class, ExperimentalCoroutinesApi::class)
class TakeStarterViewModel(
    private val db: AppDatabase,
    state: SavedStateHandle,
    private val createService: CreateService = CreateService(db),
    private val libraryService: LibraryService = LibraryService(db),
) : ViewModel() {
    val option = MutableStateFlow<SessionOption?>(null)
    var uiCoroutineScope: CoroutineScope? = null
        private set

    val takeStats by lazy {
        option.filterNotNull()
            .mapLatest { libraryService.getTakesBySession(it.id) }
            .flatMapMerge { it }
    }

    val flipCardState = FlipCardState()

    @Serializable
    data class Timer(val duration: Duration = 10.0.minutes, val id: String = randomUUID())

    val timers by state.saveable(saver = protoBufStateListSaver()) {
        mutableStateListOf<Timer>()
    }
    var currentTimer by state.saveable(stateSaver = protobufSaver()) { mutableStateOf(Timer(id = "")) }

    var currentTakeId by state.saveable { mutableLongStateOf(-1) }
        private set
    var showHidden by state.saveable { mutableStateOf(false) }
        private set

    data class Events(
        val create: Channel<Unit> = Channel(),
        val start: Channel<Long> = Channel(),
        val tapTake: Channel<Long> = Channel(),
        val flip: Channel<Int> = Channel(),
        val hide: Channel<Long> = Channel(),
        val unhide: Channel<Long> = Channel(),
        val toggleShowHidden: Channel<Unit> = Channel(),
        val createAndStart: Channel<Unit> = Channel(),
        val selectTimer: Channel<String> = Channel(),
        val updateTimerAndClose: Channel<Unit> = Channel(),
    )

    val event = Events()

    private suspend fun newTake(): Long {
        val takeId = createService.createTake(
            sessionId = option.value!!.session.id,
            timers = timers.map(
                Timer::duration
            ),
        )
        timers.clear()
        return takeId
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.create.onReceive {
                        newTake()
                    }

                    event.flip.onReceive {
                        uiCoroutineScope?.launch {
                            flipCardState.flip(it)
                        } ?: error("No ui coroutine scope specified")
                    }

                    event.start.onReceive {
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer),
                            NavigationOption.OpenTake(it)
                        )
                    }

                    event.tapTake.onReceive {
                        if (currentTakeId == it) {
                            currentTakeId = -1
                        } else {
                            currentTakeId = it
                        }
                    }

                    event.hide.onReceive {
                        db.transaction {
                            db.sessionQueries.updateTakeVisibility(
                                hidden = 1,
                                id = it
                            )
                        }
                    }

                    event.unhide.onReceive {
                        db.transaction {
                            db.sessionQueries.updateTakeVisibility(
                                hidden = 0,
                                id = it
                            )
                        }
                    }

                    event.toggleShowHidden.onReceive {
                        showHidden = !showHidden
                        Unit
                    }

                    event.createAndStart.onReceive {
                        val id = newTake()
                        currentTakeId = id
                        uiCoroutineScope?.launch {
                            flipCardState.flip(0)
                            Navigator.navigate(
                                Navigation.Goto(AppDestination.Answer),
                                NavigationOption.OpenTake(id)
                            )
                        } ?: error("No ui coroutine scope specified")
                    }

                    event.selectTimer.onReceive { id ->
                        val timer = timers.firstOrNull { it.id == id }
                        if (timer != null) {
                            currentTimer = timer
                        }
                    }

                    event.updateTimerAndClose.onReceive {
                        val index = timers.indexOfFirst { it.id == currentTimer.id }
                        if (index >= 0) {
                            timers.removeAt(index)
                            timers.add(index, currentTimer)
                        }
                        currentTimer = Timer(id = "")
                        Unit
                    }
                }
            }
        }
    }

    suspend fun load(option: SessionOption, coroutineScope: CoroutineScope) {
        uiCoroutineScope = coroutineScope
        this.option.emit(option)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TakeStarterViewModel(
                    Database.app,
                    createPlatformSavedStateHandle()
                )
            }
        }
    }
}