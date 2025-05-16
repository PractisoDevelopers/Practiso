package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.TextFrame
import kotlinx.coroutines.flow.Flow

actual fun platformGetEmbeddings(
    textFrames: Set<TextFrame>,
    imageFrames: Set<ImageFrame>,
    fei: FrameEmbeddingInference,
    parallelTasks: Int,
): Flow<InferenceState> = getEmbeddingsUnconfined(textFrames, imageFrames, fei, parallelTasks)