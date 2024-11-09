package com.zhufucdev.practiso

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationStateSnapshot
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.NavigatorStackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

abstract class NavigatorComponentActivity : ComponentActivity() {
    private lateinit var navigationOptions: List<NavigationOption>

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination =
            AppDestination.entries.firstOrNull { getActivity(it) == this::class.java }
                ?: error("Unknown app destination for ${this::class}")

        repeat(backstack.lastIndex - pointer) {
            backstack.removeAt(backstack.lastIndex)
        }
        navigationOptions =
            intent.getByteArrayExtra("options")
                ?.let { ProtoBuf.decodeFromByteArray(serializer<List<NavigationOption>>(), it) }
                ?: emptyList()

        backstack.add(NavigatorStackItem(destination, navigationOptions))
        pointer = backstack.lastIndex
        lifecycleScope.launch {
            state.emit(
                if (destination == Navigation.Home.destination) {
                    NavigationStateSnapshot(Navigation.Home, destination, navigationOptions)
                } else {
                    NavigationStateSnapshot(
                        Navigation.Goto(destination),
                        destination,
                        navigationOptions
                    )
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        shared = this
    }

    override fun finish() {
        pointer--
        lifecycleScope.launch {
            state.emit(
                NavigationStateSnapshot(
                    Navigation.Backward,
                    backstack[pointer].destination
                )
            )
        }
        super.finish()
    }

    companion object : AppNavigator {
        val backstack = mutableListOf(NavigatorStackItem(AppDestination.MainView, emptyList()))
        private var pointer: Int = 0

        private var shared: NavigatorComponentActivity? = null
        private val state =
            MutableStateFlow(NavigationStateSnapshot(Navigation.Home, Navigation.Home.destination))
        override val current: StateFlow<NavigationStateSnapshot> = state.asStateFlow()

        override suspend fun navigate(navigation: Navigation, options: List<NavigationOption>) {
            when (navigation) {
                is Navigation.Forward -> {
                    if (pointer >= backstack.lastIndex) {
                        error("Backstack cannot move forwards")
                    }
                    val dest = backstack[++pointer]
                    startActivity(dest.destination, dest.options)
                    state.emit(NavigationStateSnapshot(navigation, dest.destination, dest.options))
                }

                is Navigation.Backward -> {
                    if (pointer <= 0) {
                        error("Backstack cannot move backwards")
                    }
                    shared?.finish() ?: error("Shared activity presents nothing")
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

        @OptIn(ExperimentalSerializationApi::class)
        private fun startActivity(
            destination: AppDestination,
            options: List<NavigationOption> = emptyList(),
        ) {
            shared?.apply {
                startActivity(Intent(this, getActivity(destination)).apply {
                    putExtra("options", ProtoBuf.encodeToByteArray(serializer(), options))
                })
            } ?: error("Shared activity presents nothing")
        }
    }
}