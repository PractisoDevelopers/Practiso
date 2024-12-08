package com.zhufucdev.practiso.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Take
import com.zhufucdev.practiso.datamodel.Answer
import com.zhufucdev.practiso.datamodel.PageStyle
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.SettingsModel
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
    val settings: SettingsModel,
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

    sealed interface PageState {
        val progress: Float

        sealed class Pager : PageState {
            abstract val state: PagerState
            override val progress: Float by derivedStateOf {
                (state.currentPageOffsetFraction + state.currentPage + 1) / state.pageCount
            }

            data class Horizontal(override val state: PagerState) : Pager()
            data class Vertical(override val state: PagerState) : Pager()
        }

        data class Column(val state: LazyListState) : PageState {
            override val progress: Float by derivedStateOf {
                (
                        state.firstVisibleItemIndex
                                + state.firstVisibleItemScrollOffset
                                * 1f / (state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1)
                        ) / state.layoutInfo.totalItemsCount
            }
        }
    }

    val pageState by lazy {
        MutableStateFlow<PageState?>(null).apply {
            viewModelScope.launch {
                settings.answerPageStyle.collectLatest { style ->
                    quizzes.map {
                        it?.let { q ->
                            when (style) {
                                PageStyle.Horizontal -> {
                                    PageState.Pager.Horizontal(
                                        PagerState(currentQuizIndex) { q.size }
                                    )
                                }

                                PageStyle.Vertical -> {
                                    PageState.Pager.Vertical(
                                        PagerState(currentQuizIndex) { q.size }
                                    )
                                }

                                PageStyle.Column -> {
                                    PageState.Column(LazyListState(currentQuizIndex))
                                }
                            }
                        }
                    }
                        .collect(this@apply)
                }
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

    var currentQuizIndex: Int by state.saveable { mutableIntStateOf(0) }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.answer.onReceive {
                        db.transaction {
                            it.commit(db, takeId.value, priority = currentQuizIndex)
                        }
                    }

                    event.unanswer.onReceive {
                        db.transaction {
                            it.rollback(db, takeId.value)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AnswerViewModel(Database.app, createPlatformSavedStateHandle(), AppSettings)
            }
        }
    }
}