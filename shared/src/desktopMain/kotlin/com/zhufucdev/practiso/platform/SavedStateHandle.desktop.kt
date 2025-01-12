package com.zhufucdev.practiso.platform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

actual fun CreationExtras.createPlatformSavedStateHandle() = SavedStateHandle()