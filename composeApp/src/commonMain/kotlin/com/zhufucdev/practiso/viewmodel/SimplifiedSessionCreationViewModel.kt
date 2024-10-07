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

@OptIn(SavedStateHandleSaveableApi::class)
class SimplifiedSessionCreationViewModel(private val state: SavedStateHandle) : ViewModel() {
    var expanded by state.saveable { mutableStateOf(false) }
    var useRecommendations by state.saveable { mutableStateOf(true) }
    var transitionStart: Rect by mutableStateOf(Rect.Zero)

    companion object {
        val Factory get() = viewModelFactory {
            initializer {
                SimplifiedSessionCreationViewModel(createSavedStateHandle())
            }
        }
    }
}