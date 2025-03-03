package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.DimensionQueries
import com.zhufucdev.practiso.database.Quiz

data class QuizIntensity(val quiz: Quiz, val intensity: Double)

fun DimensionQueries.getQuizIntensitiesById(dimId: Long) =
    getQuizIntensitiesByDimensionId(dimId) { id, name, creation, modification, intensity ->
        QuizIntensity(
            quiz = Quiz(id, name, creation, modification),
            intensity = intensity
        )
    }

data class DimensionQuizzes(val dimension: Dimension, val quizzes: List<Quiz>)