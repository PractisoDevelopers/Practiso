package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.AppSettingsScope
import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.UniqueIO
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.DEFAULT_COMMUNITY_SERVER_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn

val getCommunityServiceWithDownloadManager = {
    var downloadScope: CoroutineScope? = null
    val bundle = AppSettings.communityServerUrl.combine(
        AppSettings.communityUseCustomServer,
        ::Pair
    )
        .mapLatest { (server, use) ->
            downloadScope?.cancel()
            downloadScope = CoroutineScope(Dispatchers.UniqueIO)
            Pair(
                CommunityService(
                    server.takeIf { use } ?: DEFAULT_COMMUNITY_SERVER_URL
                ),
                DownloadManager(downloadScope)
            )
        }
        .shareIn(AppSettingsScope, started = SharingStarted.Lazily, replay = 1)
    val closure: () -> Flow<Pair<CommunityService, DownloadManager>> = {
        bundle
    }
    closure
}()