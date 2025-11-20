package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import com.zhufucdev.practiso.composable.ImmediateMutationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

val LocalSharedImmediateMutation =
    compositionLocalOf { SharedImmediateMutationRegistry(coroutine = MainScope()) }

@Stable
data class SharedImmediateMutationRegistry(
    val entries: MutableMap<String, SharedImmediateMutationEntry> = mutableStateMapOf(),
    val coroutine: CoroutineScope
)

@Stable
class SharedImmediateMutationEntry(initial: Any?) {
    val state = ImmediateMutationState(initial)
    var references = 0

    @Suppress("UNCHECKED_CAST")
    fun <T> getStateOfTypeOrThrow(): ImmediateMutationState<T> = state as ImmediateMutationState<T>

    @Composable
    fun HoldingEffect(key: String) {
        val context = LocalSharedImmediateMutation.current
        DisposableEffect(true) {
            references += 1
            onDispose {
                references -= 1
                if (references <= 0) {
                    context.entries.remove(key)
                }
            }
        }
    }
}
