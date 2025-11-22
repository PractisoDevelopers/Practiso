package com.zhufucdev.practiso.datamodel

interface AppException {
    val scope: AppScope
    val appMessage: AppMessage? get() = null

    class Generic(val inner: Exception) : Exception(inner.message, inner.cause), AppException {
        override val scope: AppScope
            get() = AppScope.Unknown

        override val appMessage: AppMessage
            get() = AppMessage.GenericFailure
    }
}
