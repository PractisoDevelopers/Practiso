@file:OptIn(ExperimentalMaterial3Api::class)

package com.zhufucdev.practiso.viewmodel

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.helper.protoBufStateSetSaver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

@OptIn(SavedStateHandleSaveableApi::class)
sealed class SectionEditViewModel<T : PractisoOption>(state: SavedStateHandle) :
    ViewModel() {
    protected val _selection by state.saveable(saver = protoBufStateSetSaver<Long>()) { mutableStateSetOf() }
    val selection: Set<Long> get() = _selection

    val commonEvents = CommonEvents()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    commonEvents.select.onReceive {
                        _selection.add(it)
                    }
                    commonEvents.deselect.onReceive {
                        _selection.remove(it)
                    }
                    commonEvents.selectAll.onReceive {
                        _selection.addAll(it)
                    }
                    commonEvents.clearSelection.onReceive {
                        _selection.clear()
                    }
                }
            }
        }
    }

    data class CommonEvents(
        val select: Channel<Long> = Channel(),
        val deselect: Channel<Long> = Channel(),
        val selectAll: Channel<Collection<Long>> = Channel(),
        val clearSelection: Channel<Unit> = Channel(),
    )
}
