package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle

@OptIn(SavedStateHandleSaveableApi::class)
class QuizCreateViewModel(state: SavedStateHandle) : ViewModel() {
    var showNameEditDialog by state.saveable { mutableStateOf(false) }
    var nameEditValue by state.saveable { mutableStateOf("") }
    val imageCache = BitmapRepository()

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    QuizCreateViewModel(createPlatformSavedStateHandle())
                }
            }
    }
}