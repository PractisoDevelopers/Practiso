package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel

enum class Language {
    World, English, Chinese, German, Spanish,

    /**
     * Code, numbers, punctuations, and unknown text
     */
    Default
}

expect class LanguageIdentifier() {
    suspend fun getLanguage(text: String): Language
}

interface FrameEmbeddingInference : AutoCloseable {
    val model: MlModel
    suspend fun getEmbeddings(frame: Frame): FloatArray
    suspend fun getEmbeddings(frames: List<Frame>): List<FloatArray>
}

expect suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference
