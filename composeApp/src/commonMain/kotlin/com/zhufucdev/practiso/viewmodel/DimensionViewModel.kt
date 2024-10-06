package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.coroutines.launch

class DimensionViewModel(private val db: AppDatabase, private val state: SavedStateHandle) : ViewModel() {
    init {
        viewModelScope.launch {
            db.dimensionQueries.getAllDimensions()
        }
    }
}