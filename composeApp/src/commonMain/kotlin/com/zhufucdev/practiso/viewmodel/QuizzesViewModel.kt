package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.getQuizFrames
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class QuizzesViewModel(private val db: AppDatabase): ViewModel() {
    val quiz: Flow<List<PractisoOption.Quiz>> by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
    }

    data class Events(
        val remove: Channel<Long> = Channel()
    )
    val event = Events()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.remove.onReceive {
                        db.transaction {
                            db.quizQueries.removeQuiz(it)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            val db = Database.app
            initializer {
                QuizzesViewModel(db)
            }
        }
    }
}