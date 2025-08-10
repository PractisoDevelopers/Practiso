package com.zhufucdev.practiso.datamodel

interface AppException {
    val scope: AppScope
    val appMessage: AppMessage?
}

