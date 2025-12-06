package com.zhufucdev.practiso.datamodel

class ResourceNotFoundException(
    val resourceName: String,
    override val scope: AppScope = AppScope.LibraryIntentModel
) :
    Exception("Resource \"${resourceName}\" is absent."), AppException {
    override val appMessage: AppMessage
        get() = AppMessage.ResourceError(resourceName)
}