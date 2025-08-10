package com.zhufucdev.practiso.datamodel

class FeiException(
    override val cause: Throwable,
    override val scope: AppScope,
    override val appMessage: AppMessage?,
) : Exception(), AppException