package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.DEFAULT_COMMUNITY_SERVER_URL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object AppCommunityService : Flow<CommunityService> by AppSettings.communityServerUrl.combine(
    AppSettings.communityUseCustomServer,
    ::Pair
).map(transform = { (server, use) ->
    val endpoint = server.takeIf { use } ?: DEFAULT_COMMUNITY_SERVER_URL
    CommunityService(
        endpoint = endpoint,
        identity = AppSettings.getCommunityIdentity(endpoint)
    )
})

