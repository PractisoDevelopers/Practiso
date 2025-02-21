package com.zhufucdev.practiso.datamodel

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.DimensionQueries
import com.zhufucdev.practiso.database.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

data class QuizIntensity(val quiz: Quiz, val intensity: Double)

fun DimensionQueries.getQuizIntensitiesById(dimId: Long) =
    getQuizIntensitiesByDimensionId(dimId) { id, name, creation, modification, intensity ->
        QuizIntensity(
            quiz = Quiz(id, name, creation, modification),
            intensity = intensity
        )
    }
