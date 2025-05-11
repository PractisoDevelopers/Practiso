package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

@OptIn(SavedStateHandleSaveableApi::class)
class SharedElementTransitionPopupViewModel(state: SavedStateHandle) : ViewModel() {
    var expanded by state.saveable { mutableStateOf(false) }
        private set
    var visible by state.saveable { mutableStateOf(false) }
        private set

    var transitionStart: Rect by mutableStateOf(Rect.Zero)

    data class Events(
        val expand: Channel<Unit> = Channel(),
        val collapse: Channel<Unit> = Channel(),
        val transitionComplete: SharedFlow<Unit>,
    )

    private val transitionComplete = Channel<Unit>()
    val event = Events(
        transitionComplete = transitionComplete.receiveAsFlow().shareIn(
            viewModelScope,
            SharingStarted.Eagerly
        )
    )

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.expand.onReceive {
                        visible = true
                        delay(50)
                        expanded = true
                        transitionComplete.send(Unit)
                    }

                    event.collapse.onReceive {
                        expanded = false
                        delay(500)
                        visible = false
                        transitionComplete.send(Unit)
                    }
                }
            }
        }
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    SharedElementTransitionPopupViewModel(createPlatformSavedStateHandle())
                }
            }
    }
}