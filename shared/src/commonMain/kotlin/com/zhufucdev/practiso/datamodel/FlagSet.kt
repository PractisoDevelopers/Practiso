package com.zhufucdev.practiso.datamodel

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class IntFlagSet<T : IntFlag>(val value: Int) {
    operator fun contains(typeFlag: T): Boolean =
        value and typeFlag.value != 0
}

fun <T : IntFlag> intFlagSetOf(vararg types: T) =
    IntFlagSet<T>(types.takeIf { it.isNotEmpty() }?.map { it.value }?.reduce { acc, t -> acc or t }
        ?: 0)

interface IntFlag {
    val value: Int
}

