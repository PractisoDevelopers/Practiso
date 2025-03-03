package com.zhufucdev.practiso.datamodel

import kotlinx.serialization.Serializable

@Serializable
data class Selection(
    val quizIds: Set<Long> = emptySet(),
    val dimensionIds: Set<Long> = emptySet(),
)
