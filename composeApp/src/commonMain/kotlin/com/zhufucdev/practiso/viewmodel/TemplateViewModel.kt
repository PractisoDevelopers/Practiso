package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class TemplateViewModel(db: AppDatabase) : ViewModel() {
    val templates by lazy {
        db.templateQueries.getAllTemplates()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    TemplateViewModel(Database.app)
                }
            }
    }
}