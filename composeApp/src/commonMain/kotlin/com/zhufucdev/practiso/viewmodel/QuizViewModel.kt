package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class QuizViewModel(private val db: AppDatabase, private val state: SavedStateHandle): ViewModel() {
    val quiz: Flow<List<Quiz>> by lazy {
        db.quizQueries.getAllQuiz()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun add(name: String): Quiz {
        val t = Clock.System.now()
        val id = db.quizQueries.transactionWithResult {
            db.quizQueries.insertQuiz(name, t, t)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        return Quiz(id, name, t, t)
    }

    fun setName(id: Long, newName: String) {
        db.quizQueries.updateQuizName(newName, id)
    }

    fun setModificationTimeToNow(id: Long) {
        val t = Clock.System.now()
        db.quizQueries.updateQuizModificationTimeISO(t, id)
    }
}