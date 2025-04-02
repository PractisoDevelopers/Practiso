package com.zhufucdev.practiso.datamodel

data class ErrorModel(
    val scope: AppScope,
    val exception: Exception? = null,
    val message: ErrorMessage? = null,
)

interface ErrorMessage {
    data class Raw(val content: String) : ErrorMessage
    data class CopyResource(val requester: String, val archive: String) : ErrorMessage
    data object InvalidFileFormat : ErrorMessage
}

enum class AppScope {
    Unknown,
    LibraryIntentModel,
}

