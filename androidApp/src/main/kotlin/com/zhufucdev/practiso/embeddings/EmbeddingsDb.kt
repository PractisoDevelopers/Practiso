package com.zhufucdev.practiso.embeddings

import android.content.Context
import com.zhufucdev.practiso.platform.AndroidPlatform
import io.objectbox.BoxStore

object EmbeddingsDb {
    private lateinit var store: BoxStore

    fun initialize(context: Context) {
        store =
            MyObjectBox.builder()
                .androidContext(context)
                .build()
        AndroidPlatform.channelVectorDbDriver.frameChannel.trySend(Frame(store))
    }
}