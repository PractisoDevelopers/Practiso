package com.zhufucdev.practiso.viewmodel

import androidx.core.bundle.Bundle
import androidx.core.bundle.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.DimensionOption
import com.zhufucdev.practiso.datamodel.Selection
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.service.CreateService
import com.zhufucdev.practiso.service.RemoveService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.empty_span
import resources.x_and_n_more_para
import resources.x_and_y_span

class DimensionSectionEditVM(
    private val db: AppDatabase,
    private val removeService: RemoveService = RemoveService(db),
    private val createService: CreateService = CreateService(db),
    state: SavedStateHandle,
) : SectionEditViewModel<DimensionOption>(state) {
    data class Event(
        val removeWithQuizzes: Channel<Unit> = Channel(),
        val removeKeepQuizzes: Channel<Unit> = Channel(),
        val newTakeFromSelection: Channel<Unit> = Channel(),
    )

    fun loadStartpoint(value: Startpoint) {
        _selection.add(value.dimensionId)
    }

    val events = Event()

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    events.removeKeepQuizzes.onReceive {
                        removeService.removeDimensionKeepQuizzes(selection)
                        _selection.clear()
                    }

                    events.removeWithQuizzes.onReceive {
                        removeService.removeDimensionWithQuizzes(selection)
                        _selection.clear()
                    }

                    events.newTakeFromSelection.onReceive {
                        if (selection.isEmpty()) {
                            return@onReceive
                        }

                        val sessionId = createService.createSession(
                            describeSelection(),
                            Selection(dimensionIds = selection)
                        )
                        val takeId = createService.createTake(sessionId, emptyList())
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer),
                            NavigationOption.OpenTake(takeId)
                        )
                    }
                }
            }
        }
    }

    suspend fun describeSelection(): String {
        val dimensions = db.dimensionQueries.getDimensionsByIds(selection)
            .executeAsList()
            .sortedBy { it.name }

        if (dimensions.isEmpty()) {
            return getString(Res.string.empty_span)
        }

        val nMore = dimensions.size - 2
        return if (nMore >= 1) {
            val names = dimensions.slice(0 until 2).joinToString { it.name }
            getString(Res.string.x_and_n_more_para, names, nMore)
        } else if (nMore == 0) {
            getString(
                Res.string.x_and_y_span,
                dimensions[0].name,
                dimensions[1].name
            )
        } else {
            dimensions.first().name
        }
    }

    fun getDistinctQuizCountInSelection(): Int =
        db.dimensionQueries.getQuizCountByDimensions(selection)
            .executeAsOneOrNull()?.toInt() ?: 0

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DimensionSectionEditVM(db = Database.app, state = createPlatformSavedStateHandle())
            }
        }
    }

    @Serializable
    data class Startpoint(val dimensionId: Long, val topItemIndex: Int)

    object StartpointNavType : NavType<Startpoint>(false) {
        private const val DimensionIdKey = "dimension_id"
        private const val TopItemIndexKey = "top_item_idx"

        override fun get(
            bundle: Bundle,
            key: String,
        ): Startpoint? =
            bundle.getBundle(key)
                ?.let { Startpoint(it.getLong(DimensionIdKey), it.getInt(TopItemIndexKey)) }

        override fun parseValue(value: String): Startpoint =
            throw UnsupportedOperationException()

        override fun put(
            bundle: Bundle,
            key: String,
            value: Startpoint,
        ) {
            bundle.putBundle(
                key, bundleOf(
                    DimensionIdKey to value.dimensionId,
                    TopItemIndexKey to value.topItemIndex
                )
            )
        }
    }
}