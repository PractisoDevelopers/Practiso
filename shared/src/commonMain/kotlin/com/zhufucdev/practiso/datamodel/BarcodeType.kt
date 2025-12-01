package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.ProtocolAction
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class BarcodeType(override val value: Int) : IntFlag {
    companion object {
        val AUTHORIZATION_TOKEN = BarcodeType(1)
        val ALL = BarcodeType(Int.MAX_VALUE)
        val UNKNOWN = BarcodeType(0)
    }
}

fun String.getBarcodeType(): BarcodeType =
    try {
        when (val action = ProtocolAction.of(Url(this))) {
            is ProtocolAction.ImportAuthToken -> {
                BarcodeType.AUTHORIZATION_TOKEN
            }

            else -> BarcodeType.UNKNOWN
        }
    } catch (_: IllegalStateException) {
        BarcodeType.UNKNOWN
    }
