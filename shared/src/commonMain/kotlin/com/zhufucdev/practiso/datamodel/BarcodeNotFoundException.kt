package com.zhufucdev.practiso.datamodel

class BarcodeNotFoundException : Exception("Barcode not found"), AppException {
    override val scope: AppScope
        get() = AppScope.BarcodeScanner

    override val appMessage: AppMessage
        get() = AppMessage.BarcodeNotFound
}