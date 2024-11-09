package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

@OptIn(SavedStateHandleSaveableApi::class, ExperimentalSerializationApi::class)
class QuizViewModel(
    state: SavedStateHandle,
) : ViewModel() {

    val frames: MutableList<Frame> by state.saveable(saver = listSaver(
        save = {
            it.map { frame ->
                ProtoBuf.encodeToByteArray(serializer(), frame)
            }
        },
        restore = {
            it.map { s ->
                ProtoBuf.decodeFromByteArray(serializer<Frame>(), s)
            }.toMutableStateList()
        }
    )) {
        mutableStateListOf()
    }

    var name by state.saveable { mutableStateOf("") }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    QuizViewModel(createPlatformSavedStateHandle())
                }
            }
    }
}