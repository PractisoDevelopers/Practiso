package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhufucdev.practiso.page.QuizCreateApp
import com.zhufucdev.practiso.style.PractisoTheme

class QuizCreateActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                QuizCreateApp()
            }
        }
    }
}