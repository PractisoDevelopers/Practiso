package com.zhufucdev.practiso.datamodel

data class ErrorModel(
    val scope: AppScope,
    val error: Throwable? = null,
    val message: ErrorMessage? = null,
)

sealed class ErrorMessage {
    data class Raw(val content: String) : ErrorMessage()
    data class CopyResource(val requester: String, val archive: String) : ErrorMessage()
    data object InvalidFileFormat : ErrorMessage()
    data object IncompatibleModel : ErrorMessage()
    data class Localized(
        val resource: Any,
        val args: List<Any> = emptyList(),
    ) : ErrorMessage()
}

enum class AppScope {
    Unknown,
    LibraryIntentModel,
    FeiInitialization
}

