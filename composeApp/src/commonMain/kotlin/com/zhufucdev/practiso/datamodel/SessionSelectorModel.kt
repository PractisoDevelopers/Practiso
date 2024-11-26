package com.zhufucdev.practiso.datamodel

import androidx.compose.runtime.saveable.autoSaver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable

@OptIn(SavedStateHandleSaveableApi::class)
class SessionSelectorModel(state: SavedStateHandle) {
    val quizIds by state.saveable(saver = autoSaver()) { mutableListOf<Long>() }
    val dimensionIds by state.saveable(saver = autoSaver()) { mutableListOf<Long>() }
}