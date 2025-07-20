package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.Flow

expect fun getFeiInferenceSession(): Flow<InferenceSession>