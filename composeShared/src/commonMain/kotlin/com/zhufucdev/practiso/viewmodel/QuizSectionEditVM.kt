package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.ExportService
import com.zhufucdev.practiso.service.RemoveService
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.io.okio.asOkioSink
import kotlinx.serialization.Serializable
import okio.buffer
import okio.gzip
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.empty_span
import resources.new_question_para
import resources.new_question_span
import resources.x_and_n_more_para
import resources.x_and_y_span

class QuizSectionEditVM(
    private val db: AppDatabase = Database.app,
    private val removeService: RemoveService = RemoveService(db),
    private val exportService: ExportService = ExportService(db),
    state: SavedStateHandle,
) : SectionEditViewModel<QuizOption>(state) {
    data class Event(
        val removeSection: Channel<Unit> = Channel(),
        val exportToFile: Channel<PlatformFile> = Channel(),
    )

    val events = Event()

    fun loadStartpoint(value: Startpoint) {
        _selection.add(value.quizId)
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    events.removeSection.onReceive {
                        removeService.removeQuizWithResources(selection)
                        _selection.clear()
                    }

                    events.exportToFile.onReceive {
                        it.sink().asOkioSink().gzip()
                            .use { sink ->
                                exportService.exportAsSource(selection)
                                    .buffer()
                                    .readAll(sink)
                            }
                    }
                }
            }
        }
    }

    suspend fun describeSelection(): String {
        val quizzes = db.quizQueries.getQuizByIds(selection)
            .executeAsList()
            .sortedBy { it.name }

        if (quizzes.isEmpty()) {
            return getString(Res.string.empty_span)
        }

        val nMore = quizzes.size - 2
        return if (nMore >= 1) {
            val names = coroutineScope {
                quizzes.slice(0 until 2).mapIndexed { index, quiz ->
                    async {
                        quiz.name
                            ?: getString(
                                if (index == 0) Res.string.new_question_para
                                else Res.string.new_question_span
                            )
                    }
                }
            }.awaitAll().joinToString()
            getString(Res.string.x_and_n_more_para, names, nMore)
        } else if (nMore == 0) {
            getString(
                Res.string.x_and_y_span,
                quizzes[0].name ?: getString(Res.string.new_question_para),
                quizzes[1].name ?: getString(Res.string.new_question_span)
            )
        } else {
            quizzes.first().name ?: getString(Res.string.new_question_para)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                QuizSectionEditVM(state = createPlatformSavedStateHandle())
            }
        }
    }

    @Serializable
    data class Startpoint(val quizId: Long, val topItemIndex: Int)
}