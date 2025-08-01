package com.zhufucdev.practiso

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.NavigationStateSnapshot
import com.zhufucdev.practiso.platform.NavigatorStackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

abstract class NavigatorComponentActivity : ComponentActivity() {
    lateinit var navigationOptions: List<NavigationOption>

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination =
            AppDestination.entries.firstOrNull {
                (PractisoApp.instance as Destinationable).getActivity(
                    it
                ) == this::class.java
            }
                ?: error("Unknown app destination for ${this::class}")

        repeat(backstack.lastIndex - pointer) {
            backstack.removeAt(backstack.lastIndex)
        }
        navigationOptions =
            intent.getByteArrayExtra("options")
                ?.let {
                    ProtoBuf.Default.decodeFromByteArray(
                        serializer<List<NavigationOption>>(),
                        it
                    )
                }
                ?: emptyList()

        backstack.add(NavigatorStackItem(destination, navigationOptions) to this)
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

    override fun finish() {
        pointer--
        if (pointer >= 0) {
            lifecycleScope.launch {
                stateChannel.send(
                    NavigationStateSnapshot(
                        Navigation.Backward,
                        backstack[pointer].first.destination
                    )
                )
            }
        }
        super.finish()
    }

    companion object : AppNavigator {
        val backstack = mutableListOf<Pair<NavigatorStackItem, NavigatorComponentActivity>>()
        private var pointer: Int = 0

        private val shared: NavigatorComponentActivity?
            get() {
                val am = SharedContext.getSystemService<ActivityManager>()
                return am?.appTasks?.firstOrNull()?.taskInfo?.topActivity?.let {
                    backstack.lastOrNull { (_, activity) -> activity::class.qualifiedName == it.className }
                        ?.second
                }
            }
        private val state =
            MutableStateFlow(NavigationStateSnapshot(Navigation.Home, Navigation.Home.destination))
        override val current: StateFlow<NavigationStateSnapshot> = state.asStateFlow()

        private val stateChannel = Channel<NavigationStateSnapshot>()
        val coroutineScope = CoroutineScope(Dispatchers.Default)

        init {
            coroutineScope.launch {
                while (coroutineScope.isActive) {
                    select<Unit> {
                        stateChannel.onReceive {
                            state.emit(it)
                        }
                    }
                }
            }
        }

        override suspend fun navigate(navigation: Navigation, vararg options: NavigationOption) {
            when (navigation) {
                is Navigation.Forward -> {
                    if (pointer >= backstack.lastIndex) {
                        error("Backstack cannot move forwards")
                    }
                    val dest = backstack[++pointer]
                    startActivity(
                        dest.first.destination,
                        *dest.first.options.toTypedArray(),
                        *options
                    )
                    stateChannel.send(
                        NavigationStateSnapshot(
                            navigation,
                            dest.first.destination,
                            dest.first.options + options
                        )
                    )
                }

                is Navigation.Backward -> {
                    if (pointer <= 0) {
                        error("Backstack cannot move backwards")
                    }
                    shared?.finish() ?: error("Shared activity presents nothing")
                }

                is Navigation.WithDestination -> {
                    startActivity(navigation.destination, *options)
                }
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun startActivity(
            destination: AppDestination,
            vararg options: NavigationOption,
        ) {
            shared?.apply {
                startActivity(
                    Intent(
                        this,
                        (PractisoApp.instance as Destinationable).getActivity(destination)
                    ).apply {
                        putExtra(
                            "options",
                            ProtoBuf.Default.encodeToByteArray(serializer(), options)
                        )
                    })
            } ?: error("Shared activity presents nothing")
        }
    }
}