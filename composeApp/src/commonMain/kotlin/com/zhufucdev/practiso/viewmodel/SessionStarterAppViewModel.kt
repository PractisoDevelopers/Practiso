package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.datamodel.getFramedQuizzes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map

class SessionStarterAppViewModel(private val db: AppDatabase) : ViewModel() {
    data class Item(
        val dimension: Dimension,
        val quizzes: List<PractisoOption.Quiz>,
    )

    val items by lazy {
        db.dimensionQueries.getAllDimensions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map {
                coroutineScope {
                    it.map { dimension ->
                        async {
                            Item(
                                dimension = dimension,
                                quizzes = db.quizQueries
                                    .getFramedQuizzes(db.quizQueries.getQuizByDimension(dimension.id))
                                    .toOptionFlow()
                                    .last()
                            )
                        }
                    }.awaitAll()
                }
            }
    }

    companion object {
        val Factory = viewModelFactory {
            val db = Database.app
            initializer {
                SessionStarterAppViewModel(db)
            }
        }
    }
}

