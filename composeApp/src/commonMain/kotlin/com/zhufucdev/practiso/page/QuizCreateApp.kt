package com.zhufucdev.practiso.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuizCreateApp() {
    Scaffold {
        Box(Modifier.padding(it)) {
            Text("Hello from quiz creation app!")
        }
    }
}