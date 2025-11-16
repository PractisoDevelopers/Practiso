package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.AppSettingsScope
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.UniqueIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

val getCommunityServiceWithDownloadManager = run {
    var downloadScope: CoroutineScope? = null
    val bundle = AppCommunityService.map { service ->
        downloadScope?.cancel()
        downloadScope = CoroutineScope(Dispatchers.UniqueIO)
        Pair(
            service,
            DownloadManager(downloadScope)
        )
    }
        .shareIn(AppSettingsScope, started = SharingStarted.Lazily, replay = 1)
    val closure: () -> Flow<Pair<CommunityService, DownloadManager>> = {
        bundle
    }
    closure
}