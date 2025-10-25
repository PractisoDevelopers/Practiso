package com.zhufucdev.practiso.datamodel

import kotlinx.io.IOException

class NetworkUnavailableException(override val scope: AppScope) :
    IOException("Network unavailable"), AppException {
    override val appMessage: AppMessage
        get() = AppMessage.NetworkUnavailable
}