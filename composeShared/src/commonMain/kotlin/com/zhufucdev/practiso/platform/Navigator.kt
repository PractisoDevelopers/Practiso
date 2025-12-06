package com.zhufucdev.practiso.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.IntFlagSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Serializable
sealed class AppDestination<T> {
    abstract val resultType: KType

    @Serializable
    object MainView : AppDestination<Unit>() {
        override val resultType: KType
            get() = typeOf<Unit>()
    }

    @Serializable
    object QuizCreate : AppDestination<Unit>() {
        override val resultType: KType
            get() = typeOf<Unit>()
    }

    @Serializable
    object Answer : AppDestination<Unit>() {
        override val resultType: KType
            get() = typeOf<Unit>()
    }

    @Serializable
    object Preferences : AppDestination<Unit>() {
        override val resultType: KType
            get() = typeOf<Unit>()
    }

    @Serializable
    object QrCodeViewer : AppDestination<Unit>() {
        override val resultType: KType
            get() = typeOf<Unit>()
    }

    @Serializable
    object QrCodeScanner : AppDestination<String>() {
        override val resultType: KType
            get() = typeOf<String>()
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

    @Serializable
    data class ScanQrCodeFilter(val allowedTypes: IntFlagSet<BarcodeType>) : NavigationOption
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

    protected val internalBackstack: MutableList<BackstackEntry<*>> =
        mutableListOf(BackstackEntry(NavigatorStackItem(Navigation.Home.destination, emptyList())))
    protected var pointer = 0

    val backstack: List<BackstackEntry<*>>
        get() = internalBackstack

    init {
        coroutineScope.launch {
            state.collect {
                onNavigate(it)
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
                state.emit(
                    NavigationStateSnapshot(
                        navigation,
                        dest.navigation.destination,
                        dest.navigation.options + options
                    )
                )
            }

            is Navigation.Forward -> {
                if (pointer >= internalBackstack.lastIndex) {
                    error("Backstack is currently at the edge")
                }
                val dest = internalBackstack[++pointer]
                state.emit(
                    NavigationStateSnapshot(
                        navigation,
                        dest.navigation.destination,
                        dest.navigation.options + options
                    )
                )
            }

            is Navigation.WithResult<*> -> {
                if (pointer < internalBackstack.lastIndex) {
                    repeat(pointer - internalBackstack.lastIndex) {
                        internalBackstack.removeAt(pointer)
                    }
                }
                internalBackstack.add(
                    BackstackEntry(
                        NavigatorStackItem(
                            navigation.destination,
                            options.toList()
                        )
                    )
                )
                pointer++
                state.emit(
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
        state.filter { it.navigation is Navigation.Backward }.first()
        @Suppress("UNCHECKED_CAST")
        when (val result = internalBackstack[pointer + 1].result) {
            is StackNavigatorResult.Ok -> return result.value as T
            is StackNavigatorResult.Cancelled -> throw CancellationException()
        }
    }

    open suspend fun onNavigate(model: NavigationStateSnapshot) {
    }

    @Stable
    class BackstackEntry<Result>(val navigation: NavigatorStackItem<Result>) {
        var result by mutableStateOf<StackNavigatorResult<Result>>(StackNavigatorResult.Cancelled())
    }
}

expect val Navigator: AppNavigator