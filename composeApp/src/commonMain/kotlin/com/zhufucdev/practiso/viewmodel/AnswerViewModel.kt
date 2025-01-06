package com.zhufucdev.practiso.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
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
import com.zhufucdev.practiso.database.TimerByTake
import com.zhufucdev.practiso.datamodel.Answer
import com.zhufucdev.practiso.datamodel.PageStyle
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.datamodel.SettingsModel
import com.zhufucdev.practiso.datamodel.calculateTakeNumber
import com.zhufucdev.practiso.datamodel.getAnswersDataModel
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.protobufMutableStateFlowSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AnswerViewModel(
    private val db: AppDatabase,
    state: SavedStateHandle,
    val settings: SettingsModel,
) : ViewModel() {
    val session = MutableStateFlow<Session?>(null)
    val takeNumber by lazy {
        MutableStateFlow<Int?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                take.map { it?.id }.distinctUntilChanged().collectLatest { id ->
                    if (id != null) {
                        calculateTakeNumber(db, id)
                            .collect(this@apply)
                    } else {
                        emit(null)
                    }
                }
            }
        }
    }
    val answers: StateFlow<List<Answer>?> by lazy {
        MutableStateFlow<List<Answer>?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                take.map { it?.id }.distinctUntilChanged().collectLatest {
                    if (it != null) {
                        db.sessionQueries
                            .getAnswersDataModel(it)
                            .collect(this@apply)
                    } else {
                        emit(null)
                    }
                }
            }
        }
    }
    val take by lazy {
        MutableStateFlow<Take?>(null).apply {
            viewModelScope.launch(Dispatchers.IO) {
                @Suppress("DEPRECATION") // take is derived from takeId
                takeId.filter { it >= 0 }.distinctUntilChanged().collectLatest {
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
            viewModelScope.launch(Dispatchers.IO) {
                take.map { it?.id to it?.creationTimeISO }
                    .distinctUntilChanged()
                    .collectLatest { (id, creationTime) ->
                        emit(null)
                        if (id != null && creationTime != null) {
                            db.quizQueries.getQuizFrames(db.sessionQueries.getQuizzesByTakeId(id))
                                .map { frames -> frames.shuffled(Random(creationTime.epochSeconds)) }
                                .collect(this@apply)
                        }
                    }
            }
        }
    }
    val elapsed by lazy {
        MutableStateFlow<Duration?>(null).apply {
            viewModelScope.launch {
                take.map { it?.durationSeconds }.distinctUntilChanged().collectLatest {
                    if (it == null) {
                        emit(null)
                        return@collectLatest
                    }
                    var currentDuration = it.seconds
                    emit(currentDuration)
                    var startInstant = Clock.System.now()
                    while (true) {
                        delay(0.5.seconds)
                        val deltaT = Clock.System.now() - startInstant
                        if (deltaT > 10.seconds) {
                            continue
                        }
                        currentDuration += deltaT
                        emit(currentDuration)
                        startInstant = Clock.System.now()
                    }
                }
            }
        }
    }
    val timers by lazy {
        MutableStateFlow<List<Double>>(emptyList()).apply {
            viewModelScope.launch(Dispatchers.IO) {
                take.filterNotNull().collectLatest {
                    db.sessionQueries
                        .getTimersByTakeId(it.id)
                        .asFlow()
                        .mapToList(Dispatchers.IO)
                        .map { t -> t.map(TimerByTake::durationSeconds) }
                        .collect(this@apply)
                }
            }
        }
    }

    sealed interface PageState {
        val quizzes: List<QuizFrames>
        val progress: Float

        sealed class Pager : PageState {
            abstract val state: PagerState
            override val progress: Float by derivedStateOf {
                if (state.pageCount > 0) {
                    (state.currentPageOffsetFraction + state.currentPage + 1) / state.pageCount
                } else {
                    0f
                }
            }

            data class Horizontal(
                override val quizzes: List<QuizFrames>,
                override val state: PagerState,
            ) : Pager()

            data class Vertical(
                override val quizzes: List<QuizFrames>,
                override val state: PagerState,
            ) : Pager()
        }

        data class Column(override val quizzes: List<QuizFrames>, val state: LazyListState) :
            PageState {
            override val progress: Float by derivedStateOf {
                if (state.layoutInfo.totalItemsCount > 0) {
                    (state.firstVisibleItemIndex
                            + state.firstVisibleItemScrollOffset
                            * 1f / (state.layoutInfo.visibleItemsInfo.firstOrNull()?.size
                        ?: 1)) / state.layoutInfo.totalItemsCount
                } else {
                    0f
                }
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
                                        quizzes = q,
                                        state = PagerState(getCurrentQuizIndex()) { q.size }
                                    )
                                }

                                PageStyle.Vertical -> {
                                    PageState.Pager.Vertical(
                                        quizzes = q,
                                        state = PagerState(getCurrentQuizIndex()) { q.size }
                                    )
                                }

                                PageStyle.Column -> {
                                    PageState.Column(
                                        quizzes = q,
                                        state = LazyListState(getCurrentQuizIndex())
                                    )
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
        val updateDuration: Channel<Unit> = Channel(),
        val updateCurrentQuizIndex: Channel<Int> = Channel(),
    )

    val event = Events()

    /**
     * Avoid using this flow directly, use [take].id instead
     */
    @OptIn(SavedStateHandleSaveableApi::class)
    @Deprecated(
        message = " Avoid using this flow directly",
        replaceWith = ReplaceWith("take.map { it?.id ?: -1 }")
    )
    private val takeId
            by state.saveable(saver = protobufMutableStateFlowSaver<Long>()) { MutableStateFlow(-1L) }

    suspend fun loadNavOptions(options: List<NavigationOption>) {
        val takeId =
            (options.lastOrNull { it is NavigationOption.OpenTake } as NavigationOption.OpenTake?)?.takeId

        elapsed.emit(null)
        if (takeId != null) {
            val session =
                db.sessionQueries.getSessionByTakeId(takeId).executeAsOne()

            this.session.emit(session)

            db.transaction {
                db.sessionQueries.updateSessionAccessTime(Clock.System.now(), session.id)
                db.sessionQueries.updateTakeAccessTime(Clock.System.now(), takeId)
            }
            @Suppress("DEPRECATION")
            this.takeId.emit(takeId)
        }
    }

    suspend fun getCurrentQuizIndex(): Int {
        val take = take.filterNotNull().first()
        val quizzes = quizzes.filterNotNull().first()
        val targetId = db.transactionWithResult {
            db.sessionQueries.getCurrentQuizIdByTakeId(take.id)
                .executeAsOne()
                .currentQuizId
        }
        return if (targetId != null) {
            maxOf(0, quizzes.indexOfFirst { it.quiz.id == targetId })
        } else {
            0
        }
    }

    suspend fun updateDurationDb() {
        val take = take.filterNotNull().first()
        val elapsed = elapsed.filterNotNull().first()
        db.transaction {
            db.sessionQueries.updateTakeDuration(elapsed.inWholeSeconds, take.id)
        }
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.answer.onReceive {
                        val take = take.filterNotNull().first()
                        db.transaction {
                            it.commit(db, take.id, priority = getCurrentQuizIndex())
                        }
                    }

                    event.unanswer.onReceive {
                        val take = take.filterNotNull().first()
                        db.transaction {
                            it.rollback(db, take.id)
                        }
                    }

                    event.updateDuration.onReceive {
                        updateDurationDb()
                    }

                    event.updateCurrentQuizIndex.onReceive {
                        val quizzes = quizzes.filterNotNull().first()
                        if (it >= quizzes.size || it < 0) {
                            return@onReceive
                        }
                        val takeId = take.filterNotNull().first().id
                        db.transaction {
                            db.sessionQueries.updateCurrentQuizId(
                                currentQuizId = quizzes[it].quiz.id, id = takeId
                            )
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