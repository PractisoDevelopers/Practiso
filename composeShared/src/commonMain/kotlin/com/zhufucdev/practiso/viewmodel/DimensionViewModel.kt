package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.helper.protobufMutableStateFlowSaver
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.route.DimensionAppRouteParams
import com.zhufucdev.practiso.service.CategorizeService
import com.zhufucdev.practiso.service.ClusterService
import com.zhufucdev.practiso.service.ClusterState
import com.zhufucdev.practiso.service.FeiService
import com.zhufucdev.practiso.service.LibraryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

@OptIn(SavedStateHandleSaveableApi::class, ExperimentalCoroutinesApi::class)
class DimensionViewModel(db: AppDatabase, fei: FeiService, state: SavedStateHandle) : ViewModel() {
    private val categoryService = CategorizeService(db)
    private val libraryService = LibraryService(db)
    private val clusterService = ClusterService(db, fei)

    val dimensionId by state.saveable(saver = protobufMutableStateFlowSaver<Long>()) {
        MutableStateFlow(
            -1
        )
    }
    val dimension =
        dimensionId.flatMapLatest { if (it < 0) flowOf(null) else libraryService.getDimension(it) }
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val quizzes = dimension.flatMapLatest { dim ->
        if (dim != null) {
            libraryService.getQuizIntensities(dim.id)
        } else {
            flowOf(null)
        }
    }
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    private val _clusterState = MutableSharedFlow<ClusterState?>(replay = 1)
    val clusterState: SharedFlow<ClusterState?> get() = _clusterState

    data class Events(
        val remove: Channel<Collection<Long>> = Channel(),
        val add: Channel<Collection<Long>> = Channel(),
        val init: Channel<DimensionAppRouteParams> = Channel(),
        val generate: Channel<Unit> = Channel(),
        val update: Channel<Pair<Long, Double>> = Channel()
    )

    val event = Events()

    private suspend fun getDimensionId() = dimension.filterNotNull().first().id

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.remove.onReceive { quizIds ->
                        categoryService.disassociate(quizIds, getDimensionId())
                    }

                    event.add.onReceive { quizIds ->
                        categoryService.disassociate(quizIds, getDimensionId())
                    }

                    event.init.onReceive {
                        dimensionId.emit(it.dimensionId)
                    }

                    event.generate.onReceive {
                        _clusterState.emitAll(
                            clusterService.cluster(getDimensionId())
                                .flowOn(Dispatchers.Default)
                        )
                        _clusterState.emit(null)
                    }

                    event.update.onReceive { (quizId, intensity) ->
                        categoryService.updateIntensity(quizId, getDimensionId(), intensity)
                    }
                }
            }
        }
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    DimensionViewModel(Database.app, Database.fei, createPlatformSavedStateHandle())
                }
            }
    }
}