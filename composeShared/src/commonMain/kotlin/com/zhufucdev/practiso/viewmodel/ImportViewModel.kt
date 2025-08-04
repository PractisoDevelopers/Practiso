package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.ImportState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex

class ImportViewModel(db: AppDatabase) : ViewModel() {
    private val service = ImportService(db)
    private val _state =
        MutableStateFlow<ImportState>(ImportState.Idle(ImportState.IdleReason.Initialization))
    private val mutex = Mutex()

    val state: StateFlow<ImportState> get() = _state

    data class Events(
        val import: Channel<NamedSource> = Channel(),
        val importFinish: SharedFlow<ImportState.IdleReason>,
    )

    val event = Events(
        importFinish = _state.filterIsInstance<ImportState.Idle>().map { it.reason }
            .shareIn(
                viewModelScope,
                SharingStarted.Lazily
            )
    )

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.import.onReceive {
                        mutex.lock()
                        service.import(it).collect(_state)
                        mutex.unlock()
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ImportViewModel(Database.app)
            }
        }
    }
}