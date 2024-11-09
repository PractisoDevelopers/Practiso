package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel
import com.zhufucdev.practiso.viewmodel.QuizViewModel

class QuizCreateActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: QuizCreateViewModel = viewModel(factory = QuizCreateViewModel.Factory)
            val quizModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory)

            LaunchedEffect(model, quizModel) {
                QuizCreateApp.manipulateViewModelsWithNavigationOptions(
                    model,
                    quizModel,
                    navigationOptions
                )
            }

            PractisoTheme {
                QuizCreateApp(
                    model = model,
                    quizViewModel = quizModel
                )
            }
        }
    }
}