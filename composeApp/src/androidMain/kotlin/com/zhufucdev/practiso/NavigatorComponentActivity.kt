package com.zhufucdev.practiso

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationDestination
import com.zhufucdev.practiso.platform.and
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class NavigatorComponentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination =
            AppDestination.entries.firstOrNull { getActivity(it) == this::class.java }
                ?: error("Unknown app destination for ${this::class}")

        repeat(backstack.lastIndex - pointer) {
            backstack.removeAt(backstack.lastIndex)
        }
        backstack.add(destination)
        pointer = backstack.lastIndex
        lifecycleScope.launch {
            _navigation.emit(
                if (destination == Navigation.Home.destination) {
                    Navigation.Home and destination
                } else {
                    Navigation.Goto(destination) and destination
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        shared = this
    }

    override fun finish() {
        super.finish()
        pointer--
    }

    companion object : AppNavigator {
        val backstack = mutableListOf(AppDestination.MainView)
        private var pointer: Int = 0

        private var shared: NavigatorComponentActivity? = null
        private val _navigation = MutableStateFlow(Navigation.Home and Navigation.Home.destination)
        override val current: StateFlow<NavigationDestination> = _navigation.asStateFlow()

        override suspend fun navigate(navigation: Navigation) {
            when (navigation) {
                is Navigation.Forward -> {
                    if (pointer >= backstack.lastIndex) {
                        error("Backstack cannot move forwards")
                    }
                    val dest = backstack[++pointer]
                    startActivity(dest)
                    _navigation.emit(navigation and dest)
                }

                is Navigation.Backward -> {
                    if (pointer <= 0) {
                        error("Backstack cannot move backwards")
                    }
                    val dest = backstack[--pointer]
                    startActivity(dest)
                    _navigation.emit(navigation and dest)
                }

                is Navigation.WithDestination -> {
                    startActivity(navigation.destination)
                }
            }
        }

        fun getActivity(destination: AppDestination) =
            when (destination) {
                AppDestination.MainView -> MainActivity::class.java
                AppDestination.QuizCreate -> QuizCreateActivity::class.java
            }

        private fun startActivity(destination: AppDestination) {
            shared?.apply {
                startActivity(Intent(this, getActivity(destination)))
            } ?: error("Shared activity presents nothing")
        }
    }
}