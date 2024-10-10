package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.datamodel.FramedQuiz
import com.zhufucdev.practiso.datamodel.getFramedQuizzes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.new_question_span

class SessionViewModel(private val db: AppDatabase, private val state: SavedStateHandle) :
    ViewModel() {
    val sessions: Flow<List<Session>> =
        db.sessionQueries.getAllSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)

    val recentTakeStats: Flow<List<TakeStat>> by lazy {
        db.sessionQueries.getRecentTakeStats(5)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    // TODO: recommend based on error rates, quiz legitimacy, etc
    val smartRecommendations by lazy {
        db.quizQueries.getFramedQuizzes(db.quizQueries.getAllQuiz())
            .toSessionStarterRecommendationFlow()
    }
    val recentRecommendations by lazy {
        db.quizQueries.getFramedQuizzes(db.quizQueries.getRecentQuiz(5)).toSessionStarterRecommendationFlow()
    }
}

private typealias DbQuiz = com.zhufucdev.practiso.database.Quiz

sealed interface SessionStarterRecommendation {
    @Composable
    fun previewText(): String

    data class Quiz(val value: DbQuiz, val preview: String?) :
        SessionStarterRecommendation {
        @Composable
        override fun previewText(): String {
            return preview ?: stringResource(Res.string.new_question_span)
        }
    }
}

private fun Flow<List<FramedQuiz>>.toSessionStarterRecommendationFlow(): Flow<List<SessionStarterRecommendation>> =
    map { frames ->
        coroutineScope {
            frames.map {
                async {
                    SessionStarterRecommendation.Quiz(
                        value = it.quiz,
                        preview = it.frames.firstOrNull()?.getPreviewText()
                    )
                }
            }.awaitAll()
        }
    }