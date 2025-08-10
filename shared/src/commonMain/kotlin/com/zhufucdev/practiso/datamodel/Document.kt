package com.zhufucdev.practiso.datamodel

import okio.Source
import kotlin.time.Instant

/**
 * Document allows previewing quizzes without touching the app database.
 * @see [com.zhufucdev.practiso.service.DocumentService]
 */
data class QuizDocument(
    val name: String?,
    val frames: List<Frame>,
    val dimensions: List<DimensionDocument>,
    val resourcePool: Map<String, () -> Source>,
    val creationTime: Instant,
    val modificationTime: Instant?,
)

typealias DimensionDocument = DimensionArchive
