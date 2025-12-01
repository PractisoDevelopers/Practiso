package com.zhufucdev.practiso.datamodel

import kotlin.jvm.JvmInline

@JvmInline
value class AuthorizationToken(private val value: String) {
    override fun toString() = value
}
