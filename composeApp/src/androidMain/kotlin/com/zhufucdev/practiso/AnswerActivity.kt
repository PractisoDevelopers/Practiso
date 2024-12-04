package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.AnswerViewModel

class AnswerActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PractisoTheme {
                val model: AnswerViewModel = viewModel(factory = AnswerViewModel.Factory)
                LaunchedEffect(model) {
                    model.loadNavOptions(navigationOptions)
                }
                AnswerApp(model)
            }
        }
    }
}