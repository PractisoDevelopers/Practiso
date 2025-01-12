package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.SettingsModel
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

val AppSettings = SettingsModel(getPlatform().defaultSettings, CoroutineScope(Dispatchers.IO))