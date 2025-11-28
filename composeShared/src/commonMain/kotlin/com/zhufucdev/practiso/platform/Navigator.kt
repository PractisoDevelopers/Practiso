package com.zhufucdev.practiso.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
sealed class AppDestination<T> {
    abstract val type: KType

    @Serializable
    object MainView : AppDestination<Unit>() {
        override val type: KType
            get() = typeOf<MainView>()
    }

    @Serializable
    object QuizCreate : AppDestination<Unit>() {
        override val type: KType
            get() = typeOf<QuizCreate>()
    }

    @Serializable
    object Answer : AppDestination<Unit>() {
        override val type: KType
            get() = typeOf<Answer>()
    }

    @Serializable
    object Preferences : AppDestination<Unit>() {
        override val type: KType
            get() = typeOf<Preferences>()
    }

    @Serializable
    object QrCodeViewer : AppDestination<Unit>() {
        override val type: KType
            get() = typeOf<QrCodeViewer>()
    }

    @Serializable
    object QrCodeScanner : AppDestination<String>() {
        override val type: KType
            get() = typeOf<QrCodeScanner>()
    }
}

@Serializable
sealed interface NavigationOption {
    @Serializable
    data class OpenQuiz(val quizId: Long) : NavigationOption

    @Serializable
    data class OpenTake(val takeId: Long) : NavigationOption

    @Serializable
    data class OpenQrCode(val stringValue: String, val title: String? = null) : NavigationOption
}

sealed interface Navigation {
    interface WithResult<T> : Navigation {
        val destination: AppDestination<T>
    }

    data class Goto<T>(override val destination: AppDestination<T>) : WithResult<T>

    data object Forward : Navigation
    data object Backward : Navigation
    data object Home : WithResult<Unit> {
        override val destination get() = AppDestination.MainView
    }
}

data class NavigationStateSnapshot(
    val navigation: Navigation,
    val destination: AppDestination<*>,
    val options: List<NavigationOption> = emptyList(),
)

interface AppNavigator {
    val current: StateFlow<NavigationStateSnapshot>
    suspend fun navigate(navigation: Navigation, vararg options: NavigationOption)

    @Throws(CancellationException::class)
    suspend fun <T> navigateForResult(
        navigation: Navigation.WithResult<T>,
        vararg options: NavigationOption
    ): T
}

@Serializable
data class NavigatorStackItem<Result>(
    val destination: AppDestination<Result>,
    val options: List<NavigationOption>,
    var result: StackNavigatorResult<Result> = StackNavigatorResult.Cancelled()
)

@Serializable
sealed class StackNavigatorResult<T> {
    @Serializable
    data class Ok<T>(val value: T) : StackNavigatorResult<T>()

    @Serializable
    class Cancelled<T>() : StackNavigatorResult<T>()
}

abstract class StackNavigator(val coroutineScope: CoroutineScope) : AppNavigator {
    protected val state =
        MutableStateFlow(NavigationStateSnapshot(Navigation.Home, Navigation.Home.destination))

    override val current: StateFlow<NavigationStateSnapshot> = state.asStateFlow()

    protected val internalBackstack: MutableList<NavigatorStackItem<*>> =
        mutableListOf(NavigatorStackItem(Navigation.Home.destination, emptyList()))
    protected var pointer = 0

    val backstack: List<NavigatorStackItem<*>>
        get() = internalBackstack

    private val stateChannel = Channel<NavigationStateSnapshot>()

    init {
        coroutineScope.launch {
            while (isActive) {
                select {
                    stateChannel.onReceive {
                        state.emit(it)
                        onNavigate(state.value)
                    }
                }
            }
        }
    }

    override suspend fun navigate(navigation: Navigation, vararg options: NavigationOption) {
        when (navigation) {
            is Navigation.Backward -> {
                if (pointer <= 0) {
                    error("Backstack will become empty")
                }
                val dest = internalBackstack[--pointer]
                stateChannel.send(
                    NavigationStateSnapshot(
                        navigation,
                        dest.destination,
                        dest.options + options
                    )
                )
            }

            is Navigation.Forward -> {
                if (pointer >= internalBackstack.lastIndex) {
                    error("Backstack is currently at the edge")
                }
                val dest = internalBackstack[++pointer]
                stateChannel.send(
                    NavigationStateSnapshot(
                        navigation,
                        dest.destination,
                        dest.options + options
                    )
                )
            }

            is Navigation.WithResult<*> -> {
                if (pointer < internalBackstack.lastIndex) {
                    repeat(pointer - internalBackstack.lastIndex) {
                        internalBackstack.removeAt(pointer)
                    }
                }
                internalBackstack.add(NavigatorStackItem(navigation.destination, options.toList()))
                pointer++
                stateChannel.send(
                    NavigationStateSnapshot(
                        navigation,
                        navigation.destination,
                        options.toList()
                    )
                )
            }
        }
    }

    override suspend fun <T> navigateForResult(
        navigation: Navigation.WithResult<T>,
        vararg options: NavigationOption
    ): T {
        navigate(navigation, *options)
        @Suppress("UNCHECKED_CAST")
        when (val result = internalBackstack.last().result) {
            is StackNavigatorResult.Ok -> return result.value as T
            is StackNavigatorResult.Cancelled -> throw CancellationException()
        }
    }

    open suspend fun onNavigate(model: NavigationStateSnapshot) {
    }
}

expect val Navigator: AppNavigator