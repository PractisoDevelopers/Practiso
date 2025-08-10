package com.zhufucdev.practiso.datamodel

class DownloadException(
    override val cause: Throwable? = null,
    override val scope: AppScope,
    override val appMessage: AppMessage,
    kotlinMessage: String? = null,
) : Exception(kotlinMessage), AppException