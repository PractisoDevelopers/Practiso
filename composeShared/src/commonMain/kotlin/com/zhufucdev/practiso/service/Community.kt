package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.DEFAULT_COMMUNITY_SERVER_URL
import com.zhufucdev.practiso.SettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object AppCommunityService :
    Flow<CommunityService> by (AppSettings.communityServerEndpoint.map { endpoint ->
        CommunityService(
            endpoint = endpoint,
            identity = AppSettings.getCommunityIdentity(endpoint)
        )
    })

val SettingsModel.communityServerEndpoint
    get() = communityServerUrl.combine(
        communityUseCustomServer,
        ::Pair
    ).map { (url, use) -> url?.takeIf { use } ?: DEFAULT_COMMUNITY_SERVER_URL }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Lazily,
            initialValue = communityServerUrl.value.takeIf { communityUseCustomServer.value }
                ?: DEFAULT_COMMUNITY_SERVER_URL)