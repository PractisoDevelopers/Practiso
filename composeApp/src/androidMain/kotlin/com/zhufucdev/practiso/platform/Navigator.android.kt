package com.zhufucdev.practiso.platform

import android.content.Intent
import com.zhufucdev.practiso.MainActivity
import com.zhufucdev.practiso.QuizCreateActivity
import com.zhufucdev.practiso.SharedComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual val Navigator: AppNavigator
    get() = ActivityNavigator

object ActivityNavigator : AppNavigator {
    private val _destination = MutableStateFlow(AppDestination.MainView)
    override val current: StateFlow<AppDestination> = _destination.asStateFlow()

    override suspend fun navigate(destination: AppDestination) {
        SharedComponentActivity.shared.apply {
            startActivity(Intent(this, when (destination) {
                AppDestination.MainView -> MainActivity::class.java
                AppDestination.QuizCreate -> QuizCreateActivity::class.java
            }))
        }
        _destination.emit(destination)
    }
}