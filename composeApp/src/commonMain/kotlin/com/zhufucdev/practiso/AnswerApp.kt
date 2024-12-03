package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.datamodel.QuizFrames
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.AnswerViewModel
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.loading_quizzes_para
import practiso.composeapp.generated.resources.take_n_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerApp(model: AnswerViewModel) {
    val quizzes by model.quizzes.collectAsState()
    val state by model.pagerState.collectAsState(null)
    val currentQuiz by remember(quizzes, state) {
        derivedStateOf {
            quizzes?.let { q ->
                state?.let { s ->
                    q[s.currentPage]
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val takeNumber by model.takeNumber.collectAsState()
                        val session by model.session.collectAsState()
                        takeNumber?.let {
                            Column(Modifier.animateContentSize()) {
                                Text(stringResource(Res.string.take_n_para, it))

                                session?.let {
                                    Text(
                                        it.name,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                        }
                    }
                },
                navigationIcon = { NavigateUpButton() },
                modifier = Modifier.drawBehind {

                }
            )
        }
    ) { padding ->
        AnimatedContent(state != null) { loaded ->
            if (loaded) {
                HorizontalPager(
                    state = state!!,
                    modifier = Modifier.padding(padding)
                ) {
                    QuizContent(currentQuiz!!, model)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        PaddingSmall,
                        Alignment.CenterVertically
                    ),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    CircularProgressIndicator(Modifier.size(64.dp))
                    Text(stringResource(Res.string.loading_quizzes_para))
                }
            }
        }
    }
}

@Composable
private fun QuizContent(quiz: QuizFrames, model: AnswerViewModel) {
    LazyColumn {

    }
}