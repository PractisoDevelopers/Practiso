package com.zhufucdev.practiso.datamodel

sealed class AppMessage {
    data class Raw(val content: String) : AppMessage()
    data object GenericFailure : AppMessage()
    data object InvalidFileFormat : AppMessage()
    data object IncompatibleModel : AppMessage()
    data class HttpStatusFailure(val statusCode: Int) : AppMessage()
    data object HttpTransactionFailure : AppMessage()
    data object GenericHttpFailure : AppMessage()
    data object InsufficientSpace : AppMessage()
    data class ResourceError(
        val resource: String,
        val location: String? = null,
        val requester: String? = null,
    ) : AppMessage()
}