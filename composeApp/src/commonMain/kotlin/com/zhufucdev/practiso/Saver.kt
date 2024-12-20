package com.zhufucdev.practiso

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> protoBufStateListSaver() = listSaver(
    save = {
        it.map { v -> ProtoBuf.encodeToByteArray(serializer<T>(), v) }
    },
    restore = {
        it.map { b -> ProtoBuf.decodeFromByteArray(serializer<T>(), b) }.toMutableStateList()
    }
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> protobufSaver() = Saver<T, ByteArray>(
    save = {
        if (it == null) {
            byteArrayOf()
        } else {
            ProtoBuf.encodeToByteArray(serializer<T>(), it)
        }
    },
    restore = {
        if (it.isEmpty()) {
            null
        } else {
            ProtoBuf.decodeFromByteArray(serializer<T>(), it)
        }
    }
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> protobufMutableStateFlowSaver() = Saver<MutableStateFlow<T>, ByteArray>(
    save = {
        if (it.value == null) {
            byteArrayOf()
        } else {
            ProtoBuf.encodeToByteArray(serializer<T>(), it.value)
        }
    },
    restore = {
        if (it.isEmpty()) {
            null
        } else {
            MutableStateFlow(ProtoBuf.decodeFromByteArray(serializer<T>(), it))
        }
    }
)