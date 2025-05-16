package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.service.FeiService.Companion.MAX_BATCH_SIZE
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Calculate embeddings for text and image frames (currently only the former is available).
 *
 * - On Apple and JVM platforms, this in turn runs [getEmbeddingsUnconfined].
 * - On Android platform, this starts a foreground service.
 */
expect fun platformGetEmbeddings(
    textFrames: Set<TextFrame>,
    imageFrames: Set<ImageFrame>,
    fei: FrameEmbeddingInference,
    parallelTasks: Int,
): Flow<InferenceState>

fun getEmbeddingsUnconfined(
    textFrames: Set<TextFrame>,
    imageFrames: Set<ImageFrame>,
    fei: FrameEmbeddingInference,
    parallelTasks: Int,
): Flow<InferenceState> = channelFlow {
    val textFrames =
        if (fei.model.features.any { it is LanguageInput }) {
            textFrames
        } else {
            emptySet()
        }

    val total = textFrames.size
    val batchSize = minOf(textFrames.size / parallelTasks + 1, MAX_BATCH_SIZE)
    coroutineScope {
        val jobs = textFrames.chunked(batchSize)
            .map { textFrames ->
                val frames = textFrames.map { Frame.Text(it.id, it) }
                fei.getEmbeddings(frames)
                    .mapIndexed { idx, r -> InferenceRow(textFrameId = textFrames[idx].id) to r }
                    .also {
                        trySend(InferenceState.Inferring(frames.size, total))
                    }
            }

        val result = jobs.flatten()
        send(InferenceState.Complete(result))
    }
}

sealed class InferenceState {
    data class Complete(val results: List<Pair<InferenceRow, FloatArray>>) : InferenceState()

    data class Inferring(val done: Int, val total: Int) : InferenceState()
}

data class InferenceRow(val textFrameId: Long? = null, val imageFrameId: Long? = null)