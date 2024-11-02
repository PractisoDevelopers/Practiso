package com.zhufucdev.practiso.platform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

expect fun CreationExtras.createPlatformSavedStateHandle(): SavedStateHandle