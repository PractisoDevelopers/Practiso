package com.zhufucdev.practiso

import android.app.Activity
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppDestination.Answer
import com.zhufucdev.practiso.platform.AppDestination.MainView
import com.zhufucdev.practiso.platform.AppDestination.QuizCreate

class Application : PractisoApp() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun getActivity(destination: AppDestination): Class<out Activity> =
        when (destination) {
            MainView -> MainActivity::class.java
            QuizCreate -> QuizCreateActivity::class.java
            Answer -> AnswerActivity::class.java
        }
}