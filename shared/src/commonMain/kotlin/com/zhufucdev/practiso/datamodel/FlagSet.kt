package com.zhufucdev.practiso.datamodel

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class IntFlagSet<T : IntFlag>(val value: Int) {
    operator fun contains(typeFlag: T): Boolean =
        value and typeFlag.value != 0
}

fun <T : IntFlag> flagSetOf(vararg types: T) =
    IntFlagSet<T>(types.map { it.value }.reduce { acc, t -> acc or t })

interface IntFlag {
    val value: Int
}

