package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.createPlatformSavedStateHandle

@OptIn(SavedStateHandleSaveableApi::class)
class SearchViewModel(private val state: SavedStateHandle) : ViewModel() {
    var active by state.saveable {
        mutableStateOf(false)
    }
    var query by state.saveable {
        mutableStateOf("")
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SearchViewModel(createPlatformSavedStateHandle())
            }
        }
    }
}
