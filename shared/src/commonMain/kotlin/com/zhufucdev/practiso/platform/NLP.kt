package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.last

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
    suspend fun <T : Frame> getEmbeddings(frames: List<T>): List<FloatArray>
}

expect suspend fun createFrameEmbeddingInference(model: MlModel): Flow<InferenceModelState>

sealed class InferenceModelState {
    data class Download(
        val ongoingDownloads: Map<DownloadableFile, Float>,
        val completedFiles: List<DownloadableFile>,
        val overallProgress: Float,
    ) : InferenceModelState()

    data object PrepareDownload : InferenceModelState()
    data class Complete(val model: FrameEmbeddingInference) : InferenceModelState()
}

suspend fun Flow<InferenceModelState>.lastCompletion(): FrameEmbeddingInference =
    filterIsInstance<InferenceModelState.Complete>()
        .last()
        .model

suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference =
    createFrameEmbeddingInference(model).lastCompletion()
