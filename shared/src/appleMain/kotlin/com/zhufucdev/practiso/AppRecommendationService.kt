package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.SessionCreator
import com.zhufucdev.practiso.service.RecommendationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object AppRecommendationService {
    private val inner = RecommendationService()
    fun getCombined(): Flow<List<SessionCreator>> =
        inner.getSmartRecommendations()
            .combine(inner.getRecentRecommendations()) { smart, recent -> smart + recent}
}