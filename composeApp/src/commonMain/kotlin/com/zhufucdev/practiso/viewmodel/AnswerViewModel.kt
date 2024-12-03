package com.zhufucdev.practiso.viewmodel

import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Take
import com.zhufucdev.practiso.datamodel.Answer
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.getAnswersDataModel
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Clock
import kotlin.random.Random

class AnswerViewModel(
    private val db: AppDatabase,
    state: SavedStateHandle,
) : ViewModel() {
    val session = MutableStateFlow<Session?>(null)
    val takeNumber by lazy {
        MutableStateFlow<Int?>(null).apply {
            viewModelScope.launch {
                takeId.collectLatest { takeId ->
                    session.filterNotNull().collectLatest {
                        db.sessionQueries
                            .getTakeStatsBySessionId(it.id)
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .collect { stats ->
                                emit(stats.indexOfFirst { stat -> stat.id == takeId } + 1)
                            }
                    }
                }
            }
        }
    }
    val answers: StateFlow<List<Answer>?> by lazy {
        MutableStateFlow<List<Answer>?>(null).apply {
            viewModelScope.launch {
                takeId.collectLatest {
                    db.sessionQueries
                        .getAnswersDataModel(it)
                        .collect(this@apply)
                }
            }
        }
    }
    val take by lazy {
        MutableStateFlow<Take?>(null).apply {
            viewModelScope.launch {
                takeId.filter { it >= 0 }.collectLatest {
                    db.sessionQueries.getTakeById(it)
                        .asFlow()
                        .mapToOne(Dispatchers.IO)
                        .collect(this@apply)
                }
            }
        }
    }
    val quizzes by lazy {
        MutableStateFlow<List<QuizFrames>?>(null).apply {
            viewModelScope.launch {
                takeId.collectLatest { id ->
                    session.filterNotNull().collectLatest { s ->
                        take.filterNotNull().collectLatest { t ->
                            db.quizQueries.getQuizFrames(db.sessionQueries.getQuizzesByTakeId(id))
                                .map { frames -> frames.shuffled(Random(t.creationTimeISO.epochSeconds)) }
                                .collect(this@apply)
                        }
                    }
                }
            }
        }
    }

    val pagerState by lazy {
        MutableStateFlow<PagerState?>(null).apply {
            viewModelScope.launch {
                quizzes.map { it?.let { q -> PagerState { q.size } } }
                    .collect(this@apply)
            }
        }
    }
    val imageCache = BitmapRepository()

    data class Events(
        val answer: Channel<Answer> = Channel(),
        val unanswer: Channel<Answer> = Channel(),
    )

    val event = Events()

    val takeId = MutableStateFlow(-1L)
    suspend fun loadNavOptions(options: List<NavigationOption>) {
        val takeId =
            (options.lastOrNull { it is NavigationOption.OpenTake } as NavigationOption.OpenTake?)?.takeId

        if (takeId != null) {
            val session =
                db.sessionQueries.getSessionByTakeId(takeId).executeAsOne()

            this.session.emit(session)

            db.transaction {
                db.sessionQueries.updateSessionAccessTime(Clock.System.now(), session.id)
                db.sessionQueries.updateTakeAccessTime(Clock.System.now(), takeId)
            }
            this.takeId.emit(takeId)
        }
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.answer.onReceive {
                        val priority = pagerState.value!!.currentPage
                        db.transaction {
                            it.commit(db, takeId.value, priority)
                        }
                    }

                    event.unanswer.onReceive {
                        db.transaction {
                            db.sessionQueries.removeAnswer(it.quizId, takeId.value)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AnswerViewModel(Database.app, createPlatformSavedStateHandle())
            }
        }
    }
}