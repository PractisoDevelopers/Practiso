package com.zhufucdev.practiso.datamodel

class ImportException(
    override val cause: Throwable? = null,
    override val scope: AppScope,
    override val appMessage: AppMessage?,
) : Exception(), AppException