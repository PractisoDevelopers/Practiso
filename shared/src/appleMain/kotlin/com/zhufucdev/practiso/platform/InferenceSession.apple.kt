package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun getFeiInferenceSession(): Flow<InferenceSession> = flowOf(InferenceSession.default)