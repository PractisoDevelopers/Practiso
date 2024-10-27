package com.zhufucdev.practiso

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

expect fun CreationExtras.createPlatformSavedStateHandle(): SavedStateHandle