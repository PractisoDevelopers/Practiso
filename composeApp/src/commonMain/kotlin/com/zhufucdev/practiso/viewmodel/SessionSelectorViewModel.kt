package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle

@OptIn(SavedStateHandleSaveableApi::class)
class SessionSelectorViewModel(state: SavedStateHandle) : ViewModel() {
    var quizIds by state.saveable { mutableStateOf(emptyList<Int>()) }
    var dimensionIds by state.saveable { mutableStateOf(emptyList<Int>()) }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    SessionSelectorViewModel(createPlatformSavedStateHandle())
                }
            }
    }
}