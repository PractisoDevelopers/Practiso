package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.ImportState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex

class ImportViewModel(db: AppDatabase) : ViewModel() {
    val service = ImportService(db)
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    private val mutex = Mutex()

    val state: StateFlow<ImportState> get() = _state

    data class Events(
        val import: Channel<Importable> = Channel(),
    )

    val event = Events()

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