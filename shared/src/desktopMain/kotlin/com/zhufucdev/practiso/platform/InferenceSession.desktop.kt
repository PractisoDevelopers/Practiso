package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun getFeiInferenceSession(): Flow<InferenceSession> =
    AppSettings.feiCompatibilityMode.map {
        InferenceSession()
    }