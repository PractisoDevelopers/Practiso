package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.QuizCreateViewModel

class QuizCreateActivity : NavigatorComponentActivity<Unit>(AppDestination.QuizCreate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: QuizCreateViewModel = viewModel(factory = QuizCreateViewModel.Factory)

            LaunchedEffect(model) {
                model.loadNavOptions(navigationOptions)
            }

            PractisoTheme {
                QuizCreateApp(model)
            }
        }
    }
}