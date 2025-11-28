package com.zhufucdev.practiso

import android.app.Activity
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppDestination.Answer
import com.zhufucdev.practiso.platform.AppDestination.MainView
import com.zhufucdev.practiso.platform.AppDestination.Preferences
import com.zhufucdev.practiso.platform.AppDestination.QrCodeScanner
import com.zhufucdev.practiso.platform.AppDestination.QrCodeViewer
import com.zhufucdev.practiso.platform.AppDestination.QuizCreate

class Application : PractisoApp(), Destinationable {
    override fun onCreate() {
        super.onCreate()
    }

    override fun getActivity(destination: AppDestination<*>): Class<out Activity> =
        when (destination) {
            is MainView -> MainActivity::class.java
            is QuizCreate -> QuizCreateActivity::class.java
            is Answer -> AnswerActivity::class.java
            is Preferences -> PreferencesActivity::class.java
            is QrCodeViewer -> QrCodeViewerActivity::class.java
            is QrCodeScanner -> QrCodeScannerActivity::class.java
        }
}