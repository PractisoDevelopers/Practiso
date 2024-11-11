package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.BitmapRepository
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.insertTo
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.protoBufStateListSaver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

@OptIn(SavedStateHandleSaveableApi::class)
class QuizCreateViewModel(state: SavedStateHandle) : ViewModel() {
    var showNameEditDialog by state.saveable { mutableStateOf(false) }
    var nameEditValue by state.saveable { mutableStateOf("") }
    val imageCache = BitmapRepository()
    var state by state.saveable { mutableStateOf(State.Pending) }
    var lastFrameId by state.saveable { mutableStateOf(0L) }
    var name by state.saveable { mutableStateOf("") }
    val frames: List<Frame> get() = _frames

    private val _frames: MutableList<Frame> by state.saveable(saver = protoBufStateListSaver()) { mutableStateListOf() }
    var saving by state.saveable { mutableStateOf(false) }
        private set

    private val history by state.saveable(saver = protoBufStateListSaver()) { mutableStateListOf<Edit>() }
    private var head by state.saveable { mutableIntStateOf(-1) }
    val canUndo by derivedStateOf { head >= 0 }
    val canRedo by derivedStateOf { history.lastIndex > head }

    data class EventChannels(
        val add: Channel<Frame> = Channel(),
        val remove: Channel<Frame> = Channel(),
        val rename: Channel<String> = Channel(),
        val update: Channel<Frame> = Channel(),
        val undo: Channel<Unit> = Channel(),
        val redo: Channel<Unit> = Channel(),
        val save: Channel<Unit> = Channel(),
    )

    val event = EventChannels()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.add.onReceive {
                        _frames.add(it)
                        history.add(Edit.Append(it, _frames.lastIndex))
                        head++
                    }

                    event.remove.onReceive {
                        val index = _frames.indexOf(it)
                        if (index < 0) {
                            error("Removing frame was not found")
                        }

                        _frames.removeAt(index)
                        history.add(Edit.Remove(it, index))
                    }

                    event.update.onReceive { new ->
                        val oldIndex = _frames.indexOfFirst { it.id == new.id }
                        val old = _frames[oldIndex]
                        _frames[oldIndex] = new
                        history.add(Edit.Update(old, new))
                        head = history.lastIndex
                        Unit
                    }

                    event.rename.onReceive {
                        history.add(Edit.Rename(name, it))
                        name = it
                        Unit
                    }

                    event.undo.onReceive {
                        when (val current = history[head]) {
                            is Edit.Append -> current.undo()
                            is Edit.Remove -> current.undo()
                            is Edit.Rename -> current.undo()
                            is Edit.Update -> current.undo()
                        }
                        head--
                    }

                    event.redo.onReceive {
                        head++
                        when (val current = history[head]) {
                            is Edit.Append -> current.redo()
                            is Edit.Remove -> current.redo()
                            is Edit.Rename -> current.redo()
                            is Edit.Update -> current.redo()
                        }
                    }

                    event.save.onReceive {
                        if (targetId < 0) {
                            frames.insertTo(Database.app, name)
                        }
                    }
                }
            }
        }
    }

    private var targetId: Long by state.saveable { mutableLongStateOf(-1) }
    suspend fun loadNavOptions(navigationOptions: List<NavigationOption>) {
        state = State.Pending
        history.clear()

        val openQuiz = navigationOptions.filterIsInstance<NavigationOption.OpenQuiz>()
        if (openQuiz.isNotEmpty()) {
            _frames.clear()
            targetId = openQuiz.last().quizId
            val quiz = Database.app.quizQueries
                .getQuizFrames(Database.app.quizQueries.getQuizById(targetId))
                .first()
                .firstOrNull()
            if (quiz == null) {
                state = State.NotFound
            } else {
                name = quiz.quiz.name ?: ""
                _frames.addAll(quiz.frames.map(PrioritizedFrame::frame))

                nameEditValue = name
                state = State.Ready
            }
        } else {
            state = State.Ready
        }
    }

    private fun Edit.Append.undo() {
        _frames.removeAt(insertIndex)
    }

    private fun Edit.Append.redo() {
        _frames.add(insertIndex, frame)
    }

    private fun Edit.Rename.undo() {
        name = old
    }

    private fun Edit.Rename.redo() {
        name = new
    }

    private fun Edit.Update.undo() {
        val index = _frames.indexOfFirst { it.id == new.id }
        if (index < 0) {
            error("Target frame not found")
        }
        _frames[index] = old
    }

    private fun Edit.Update.redo() {
        val index = _frames.indexOfFirst { it.id == old.id }
        if (index < 0) {
            error("Target frame not found")
        }
        _frames[index] = new
    }

    private fun Edit.Remove.undo() {
        _frames.add(oldIndex, frame)
    }

    private fun Edit.Remove.redo() {
        _frames.removeAt(oldIndex)
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    QuizCreateViewModel(createPlatformSavedStateHandle())
                }
            }
    }

    enum class State {
        Pending,
        Ready,
        NotFound
    }

    @Serializable
    sealed interface Edit {
        @Serializable
        data class Append(val frame: Frame, val insertIndex: Int) : Edit

        @Serializable
        data class Remove(val frame: Frame, val oldIndex: Int) : Edit

        @Serializable
        data class Update(val old: Frame, val new: Frame) : Edit

        @Serializable
        data class Rename(val old: String, val new: String) : Edit
    }
}