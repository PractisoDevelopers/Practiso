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
import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.PageStyle
import com.zhufucdev.practiso.SettingsModel
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.datamodel.PractisoAnswer
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.TakeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AnswerViewModel(
    state: SavedStateHandle,
    val settings: SettingsModel,
) : ViewModel() {
    /**
     * Avoid using this flow directly, use [take].id instead
     */
    @OptIn(SavedStateHandleSaveableApi::class)
    @Deprecated(
        message = "Avoid using this flow directly",
        replaceWith = ReplaceWith("take.map { it?.id ?: -1 }")
    )
    private val takeId
            by state.saveable(saver = protobufMutableStateFlowSaver<Long>()) { MutableStateFlow(-1L) }

    val takeService: StateFlow<TakeService?> by lazy {
        MutableStateFlow<TakeService?>(null).apply {
            viewModelScope.launch {
                @Suppress("DEPRECATION") // take service has to be derived from takeId
                takeId.map { it.takeIf { it >= 0 }?.let { TakeService(takeId = it) } }
                    .collect(this@apply)
            }
        }
    }

    val session = takeService.map { it?.getSession() }.flatMapLatest { it ?: flowOf(null) }
    val takeNumber: Flow<Int?> =
        takeService.map { it?.getTakeNumber() }.flatMapLatest { it ?: flowOf(null) }

    private val upstreamAnswers =
        takeService.map { it?.getAnswers() }.flatMapLatest { it ?: flowOf(null) }
            .flowOn(Dispatchers.IO)
    private val cachedAnswers = MutableStateFlow<List<PractisoAnswer>?>(null).apply {
        viewModelScope.launch {
            emit(upstreamAnswers.filter { it != null }.first())
            upstreamAnswers.debounce(5.seconds).collectLatest {
                emit(it)
            }
        }
    }
    val answers get() = cachedAnswers

    val take = takeService.map { it?.getTake() }.flatMapLatest { it ?: flowOf(null) }
    val quizzes =
        takeService.map { it?.getQuizzes() }
            .flatMapLatest { it ?: flowOf(null) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    val timers: Flow<List<Double>> =
        takeService.map { it?.getTimersInSecond() }
            .distinctUntilChanged()
            .flatMapLatest { it ?: flowOf(emptyList()) }
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
        settings.answerPageStyle
            .map { style ->
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
            }
            .flatMapMerge { it }
    }
    val imageCache = BitmapRepository()

    data class Events(
        val answer: Channel<PractisoAnswer> = Channel(),
        val unanswer: Channel<PractisoAnswer> = Channel(),
        val updateDuration: Channel<Unit> = Channel(),
        val updateCurrentQuizIndex: Channel<Int> = Channel(),
    )

    val event = Events()

    suspend fun loadNavOptions(options: List<NavigationOption>) {
        val takeId =
            (options.lastOrNull { it is NavigationOption.OpenTake } as NavigationOption.OpenTake?)?.takeId

        elapsed.emit(null)
        if (takeId != null) {
            @Suppress("DEPRECATION") // this is the data source
            this.takeId.emit(takeId)

            val service = takeService.first() ?: return
            service.updateAccessTime()
        }
    }

    suspend fun getCurrentQuizIndex(): Int {
        val targetId = takeService.first()?.getCurrentQuizId() ?: return 0
        return maxOf(
            0,
            quizzes.first()?.indexOfFirst { it.quiz.id == targetId } ?: 0
        )
    }

    suspend fun updateDurationDb() {
        val elapsed = elapsed.value ?: return
        takeService.value?.updateDuration(elapsed.inWholeSeconds)
    }


    init {
        val answerDbLock = Mutex()

        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select<Unit> {
                    event.answer.onReceive {
                        cachedAnswers.update { cache ->
                            if (cache == null) listOf(it)
                            else cache + it
                        }
                        async {
                            answerDbLock.withLock {
                                takeService.value?.commitAnswer(it, getCurrentQuizIndex())
                            }
                        }
                    }

                    event.unanswer.onReceive {
                        cachedAnswers.update { cache ->
                            if (cache == null) error("Unexpected null cache")
                            else cache - it
                        }
                        async {
                            answerDbLock.withLock {
                                takeService.value?.rollbackAnswer(it)
                            }
                        }
                    }

                    event.updateDuration.onReceive {
                        updateDurationDb()
                    }

                    event.updateCurrentQuizIndex.onReceive {
                        val quizzes = quizzes.first()
                        if (quizzes == null || it >= quizzes.size || it < 0) {
                            return@onReceive
                        }
                        takeService.value?.updateCurrentQuizId(quizzes[it].quiz.id)
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AnswerViewModel(createPlatformSavedStateHandle(), AppSettings)
            }
        }
    }
}