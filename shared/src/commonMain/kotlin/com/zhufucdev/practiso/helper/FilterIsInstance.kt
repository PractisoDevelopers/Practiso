package com.zhufucdev.practiso.helper

inline fun <reified K> Iterable<*>.filterFirstIsInstanceOrNull(): K? =
    firstOrNull { it is K } as K?