package com.zhufucdev.practiso.datamodel

import kotlinx.datetime.Instant
import okio.Source

data class QuizDocument(
    val name: String?,
    val frames: List<Frame>,
    val dimensions: List<DimensionDocument>,
    val resourcePool: Map<String, () -> Source>,
    val creationTime: Instant,
    val modificationTime: Instant?,
)

typealias DimensionDocument = DimensionArchive
