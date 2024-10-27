package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.createPlatformSavedStateHandle
import kotlinx.coroutines.delay

@OptIn(SavedStateHandleSaveableApi::class)
class SimplifiedSessionCreationViewModel(private val state: SavedStateHandle) : ViewModel() {
    var expanded by state.saveable { mutableStateOf(false) }
        private set
    var visible by state.saveable { mutableStateOf(false) }
        private set

    var useRecommendations by state.saveable { mutableStateOf(true) }
    var transitionStart: Rect by mutableStateOf(Rect.Zero)

    suspend fun expand() {
        visible = true
        delay(10)
        expanded = true
    }

    suspend fun collapse() {
        expanded = false
        delay(500)
        visible = false
    }

    companion object {
        val Factory get() = viewModelFactory {
            initializer {
                SimplifiedSessionCreationViewModel(createPlatformSavedStateHandle())
            }
        }
    }
}