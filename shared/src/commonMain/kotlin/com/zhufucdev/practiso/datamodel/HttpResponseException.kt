package com.zhufucdev.practiso.datamodel

class HttpResponseException(status: Int, override val scope: AppScope, cause: Throwable? = null) :
    Exception("HTTP request responded with code $status", cause), AppException {
    override val appMessage: AppMessage = AppMessage.HttpStatusFailure(status)
}
