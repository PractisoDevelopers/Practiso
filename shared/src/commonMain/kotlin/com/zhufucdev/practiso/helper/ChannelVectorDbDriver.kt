package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.datamodel.EmbeddingsDatabase
import com.zhufucdev.practiso.datamodel.VectorDatabaseDriver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex

class ChannelVectorDbDriver : VectorDatabaseDriver {
    private val frameChan = Channel<EmbeddingsDatabase>()
    private val mutex = Mutex()

    private var frameDb: EmbeddingsDatabase? = null

    override suspend fun createFrameDb(): EmbeddingsDatabase {
        try {
            mutex.lock()
            if (frameDb == null) {
                frameDb = frameChan.receive()
            }
            return frameDb!!
        } finally {
            mutex.unlock()
        }
    }

    val frameChannel: SendChannel<EmbeddingsDatabase> get() = frameChan
}