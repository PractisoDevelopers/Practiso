package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class QuizViewModel(private val db: AppDatabase, private val state: SavedStateHandle): ViewModel() {
    private val _quiz = SnapshotStateList<Quiz>()
    val quiz: List<Quiz> get() = _quiz

    init {
        viewModelScope.launch {
            db.quizQueries.getAllQuiz()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .collect {
                    _quiz.clear()
                    _quiz.addAll(it)
                }
        }
    }

    fun add(name: String): Quiz {
        val t = Clock.System.now()
        val id = db.quizQueries.transactionWithResult {
            db.quizQueries.insertQuiz(name, t, t)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        val model = Quiz(id, name, t, t)
        _quiz.add(model)
        return model
    }

    fun setName(id: Long, newName: String) {
        val index = _quiz.indexOfFirst { it.id == id }
        if (index < 0) {
            throw IllegalArgumentException("Quiz with id $id does not exist")
        }
        db.quizQueries.updateQuizName(newName, id)
        _quiz[index] = _quiz[index].copy(name = newName)
    }

    fun setModificationTimeToNow(id: Long) {
        val index = _quiz.indexOfFirst { it.id == id }
        if (index < 0) {
            throw IllegalArgumentException("Quiz with id $id does not exist")
        }
        val t = Clock.System.now()
        db.quizQueries.updateQuizModificationTimeISO(t, id)
        _quiz[index] = _quiz[index].copy(modificationTimeISO = t)
    }
}