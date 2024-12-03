package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.viewmodel.AnswerViewModel

class AnswerActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val takeId = navigationOptions.filterIsInstance<NavigationOption.OpenTake>().last().takeId
        setContent {
            val model: AnswerViewModel = viewModel(factory = AnswerViewModel.factory(takeId))
            AnswerApp(model)
        }
    }
}