package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.ErrorMessage
import com.zhufucdev.practiso.datamodel.ErrorModel
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.service.FeiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach

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

expect fun createFrameEmbeddingInference(
    model: MlModel,
    session: InferenceSession = InferenceSession.default,
): Flow<InferenceModelState>

sealed class InferenceModelState {
    data class Download(
        val ongoingDownloads: Map<DownloadableFile, Float>,
        val completedFiles: List<DownloadableFile>,
        val overallProgress: Float,
    ) : InferenceModelState()

    data class PlanDownload(
        val files: List<DownloadableFile>,
        val build: suspend (Configuration.() -> Unit) -> Unit,
    ) : InferenceModelState()

    data class Complete(val model: FrameEmbeddingInference) : InferenceModelState()
}

suspend fun Flow<InferenceModelState>.lastCompletion(): FrameEmbeddingInference =
    filterIsInstance<InferenceModelState.Complete>()
        .last()
        .model

suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference =
    createFrameEmbeddingInference(model).onEach {
        when (it) {
            is InferenceModelState.PlanDownload -> {
                it.build {
                    discretion = DownloadDiscretion.Immediate
                }
            }

            else -> {}
        }
    }.lastCompletion()

expect class InferenceSession {
    companion object {
        val default: InferenceSession
    }
}

fun DirectoryWalker.throwsFeiError(): DirectoryWalker = object : DirectoryWalker {
    override val identifier: String
        get() = this@throwsFeiError.identifier
    override val files: Flow<DownloadableFile>
        get() = this@throwsFeiError.files
            .catch {
                throw FeiException(
                    error = ErrorModel(
                        scope = AppScope.FeiResource,
                        message = ErrorMessage.ResourceNotFound(
                            this@throwsFeiError::class.simpleName ?: "Directory Walker",
                            identifier
                        ),
                        cause = it
                    )
                )
            }
}