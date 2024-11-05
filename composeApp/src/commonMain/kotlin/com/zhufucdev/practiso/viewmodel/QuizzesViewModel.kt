package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.datamodel.getQuizFrames
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class QuizzesViewModel(private val db: AppDatabase): ViewModel() {
    val quiz: Flow<List<PractisoOption.Quiz>> by lazy {
        db.quizQueries.getQuizFrames(db.quizQueries.getAllQuiz())
            .toOptionFlow()
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

    companion object {
        val Factory = viewModelFactory {
            val db = Database.app
            initializer {
                QuizzesViewModel(db)
            }
        }
    }
}