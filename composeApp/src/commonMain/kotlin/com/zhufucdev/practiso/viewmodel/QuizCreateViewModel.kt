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
import com.zhufucdev.practiso.datamodel.Edit
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.PrioritizedFrame
import com.zhufucdev.practiso.datamodel.getQuizFrames
import com.zhufucdev.practiso.datamodel.applyTo
import com.zhufucdev.practiso.datamodel.insertInto
import com.zhufucdev.practiso.datamodel.optimized
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.protoBufStateListSaver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

@OptIn(SavedStateHandleSaveableApi::class)
class QuizCreateViewModel(state: SavedStateHandle) : ViewModel() {
    var showNameEditDialog by state.saveable { mutableStateOf(false) }
    var nameEditValue by state.saveable { mutableStateOf("") }
    val imageCache = BitmapRepository()
    var state by state.saveable { mutableStateOf(State.Pending) }
    var lastFrameId by state.saveable { mutableStateOf(0L) }
    private val _frames: MutableList<Frame> by state.saveable(saver = protoBufStateListSaver()) { mutableStateListOf() }
    val frames: List<Frame> get() = _frames
    var name by state.saveable { mutableStateOf("") }
        private set

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

    private fun addEdit(edit: Edit) {
        history.removeRange(head + 1, history.size)
        history.add(edit)
        head++
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.add.onReceive {
                        _frames.add(it)
                        addEdit(Edit.Append(it, _frames.lastIndex))
                    }

                    event.remove.onReceive {
                        val index = _frames.indexOf(it)
                        if (index < 0) {
                            error("Removing frame was not found")
                        }

                        _frames.removeAt(index)
                        addEdit(Edit.Remove(it, index))
                    }

                    event.update.onReceive { new ->
                        val oldIndex = _frames.indexOfFirst { it::class == new::class && it.id == new.id }
                        val old = _frames[oldIndex]
                        _frames[oldIndex] = new
                        addEdit(Edit.Update(old, new))
                        head = history.lastIndex
                        Unit
                    }

                    event.rename.onReceive {
                        addEdit(Edit.Rename(name, it))
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
                            frames.insertInto(Database.app, name)
                        } else {
                            history.optimized().also { println(it) }.applyTo(Database.app, targetId)
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
        _frames.clear()
        head = -1

        val openQuiz = navigationOptions.filterIsInstance<NavigationOption.OpenQuiz>()
        if (openQuiz.isNotEmpty()) {
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
                lastFrameId = frames.maxOf(Frame::id)
            }
        } else {
            state = State.Ready
            targetId = -1
            lastFrameId = -1
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
        val index = _frames.indexOfFirst { it::class == new::class && it.id == new.id }
        if (index < 0) {
            error("Target frame not found")
        }
        _frames[index] = old
    }

    private fun Edit.Update.redo() {
        val index = _frames.indexOfFirst { it::class == new::class && it.id == old.id }
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

}