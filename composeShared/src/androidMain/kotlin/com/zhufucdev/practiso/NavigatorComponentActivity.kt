package com.zhufucdev.practiso

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.AppNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.NavigationStateSnapshot
import com.zhufucdev.practiso.platform.NavigatorStackItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

abstract class NavigatorComponentActivity<Result>(protected val destination: AppDestination<Result>) :
    ComponentActivity(), ForActivityResultLaunchable {
    lateinit var navigationOptions: List<NavigationOption>

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        repeat(backstack.lastIndex - pointer) {
            backstack.removeAt(backstack.lastIndex)
        }
        navigationOptions =
            intent.getByteArrayExtra(KEY_OPTIONS)
                ?.let {
                    ProtoBuf.decodeFromByteArray(
                        serializer<List<NavigationOption>>(),
                        it
                    )
                }
                ?: emptyList()

        backstack.add(BackstackEntry(NavigatorStackItem(destination, navigationOptions), this))
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
                        backstack[pointer].item.destination
                    )
                )
            }
        }
        super.finish()
    }

    @OptIn(ExperimentalSerializationApi::class)
    protected fun setResult(value: Result) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(
                KEY_RESULT,
                ProtoBuf.encodeToByteArray(serializer(destination.type), value)
            )
        })
    }

    private val activityResultChannel = Channel<ActivityResult>()
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activityResultChannel.trySend(it)
        }

    override suspend fun startActivityForResult(intent: Intent): ActivityResult {
        activityResultLauncher.launch(intent)
        return activityResultChannel.receive()
    }


    companion object : AppNavigator {
        data class BackstackEntry<Result>(
            val item: NavigatorStackItem<Result>,
            val activity: NavigatorComponentActivity<Result>
        )

        private val backstack = mutableListOf<BackstackEntry<*>>()
        private var pointer: Int = 0

        private val shared: NavigatorComponentActivity<*>?
            get() {
                val am = SharedContext.getSystemService<ActivityManager>()
                return am?.appTasks?.firstOrNull()?.taskInfo?.topActivity?.let {
                    backstack.lastOrNull { (_, activity) -> activity::class.qualifiedName == it.className }
                        ?.activity
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
                        dest.item.destination,
                        *dest.item.options.toTypedArray(),
                        *options
                    )
                    stateChannel.send(
                        NavigationStateSnapshot(
                            navigation,
                            dest.item.destination,
                            dest.item.options + options
                        )
                    )
                }

                is Navigation.Backward -> {
                    if (pointer <= 0) {
                        error("Backstack cannot move backwards")
                    }
                    shared?.finish() ?: error("Shared activity presents nothing")
                }

                is Navigation.WithResult<*> -> {
                    startActivity(navigation.destination, *options)
                }
            }
        }

        override suspend fun <T> navigateForResult(
            navigation: Navigation.WithResult<T>,
            vararg options: NavigationOption
        ): T =
            when (navigation.destination) {
                is AppDestination.QrCodeScanner -> {
                    @Suppress("UNCHECKED_CAST")
                    startActivityForResult(
                        serializer(navigation.destination.type) as KSerializer<T>,
                        navigation.destination,
                        *options
                    )
                }

                else -> {
                    startActivity(navigation.destination, *options)
                    @Suppress("UNCHECKED_CAST")
                    Unit as T
                }
            }

        private fun startActivity(
            destination: AppDestination<*>,
            vararg options: NavigationOption,
        ) {
            shared?.apply {
                startActivity(getIntentFor(destination, options.toList()))
            } ?: error("Shared activity presents nothing")
        }

        @OptIn(ExperimentalSerializationApi::class)
        private suspend fun <T> startActivityForResult(
            serializer: DeserializationStrategy<T>,
            destination: AppDestination<T>,
            vararg options: NavigationOption,
        ): T {
            val shared = shared ?: error("Shared activity presents nothing")
            val result =
                shared.startActivityForResult(shared.getIntentFor(destination, options.toList()))
            if (result.resultCode == RESULT_CANCELED) {
                throw CancellationException()
            }
            assert(result.resultCode == RESULT_OK)
            val data = result.data?.getByteArrayExtra(KEY_RESULT) ?: error("Unexpected null result")
            return ProtoBuf.decodeFromByteArray(
                serializer,
                data
            )
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun Context.getIntentFor(
            destination: AppDestination<*>,
            options: List<NavigationOption>
        ): Intent {
            return Intent(
                this,
                (PractisoApp.instance as Destinationable).getActivity(destination)
            ).apply {
                putExtra(
                    KEY_OPTIONS,
                    ProtoBuf.encodeToByteArray(serializer(), options)
                )
            }
        }

        private const val KEY_OPTIONS = "options"
        private const val KEY_RESULT = "result"
    }
}