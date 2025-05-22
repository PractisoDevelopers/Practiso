package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.SettingsModel
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

val AppSettings = with(CoroutineScope(Dispatchers.IO)) {
    val model = SettingsModel(getPlatform().defaultSettings, this)
    launch {
        model.feiModelIndex.collectLatest { modelIdx ->
            Database.fei.setFeiModel(KnownModels[modelIdx])
        }
    }
    model
}