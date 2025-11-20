package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.zhufucdev.practiso.composition.LocalSharedImmediateMutation
import com.zhufucdev.practiso.composition.SharedImmediateMutationEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun <T> ImmediateMutation(
    model: T,
    state: ImmediateMutationState<T> = rememberImmediateMutationState(model),
    transactionCoroutineScope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable ImmediateMutationContentScope<T>.(T) -> Unit
) {
    assert(state is ImmediateMutationStateImpl)
    content(
        object : ImmediateMutationContentScope<T> {
            override fun transaction(block: suspend ImmediateMutationTransactionScope<T>.() -> Unit) {
                transactionCoroutineScope.launch {
                    try {
                        block(object : ImmediateMutationTransactionScope<T> {
                            override fun mutateValue(newValue: T) {
                                (state as ImmediateMutationStateImpl<T>).model = newValue
                            }
                        })
                    } catch (_: CancellationException) {
                        (state as ImmediateMutationStateImpl<T>).model = model
                    }
                }
            }
        },
        state.model,
    )
}

interface ImmediateMutationContentScope<T> {
    fun transaction(block: suspend ImmediateMutationTransactionScope<T>.() -> Unit)
}

interface ImmediateMutationTransactionScope<T> {
    fun mutateValue(newValue: T)
}

@Stable
interface ImmediateMutationState<T> {
    val model: T
}

@Stable
private class ImmediateMutationStateImpl<T>(initial: T) : ImmediateMutationState<T> {
    override var model: T by mutableStateOf(initial)
}

fun <T> ImmediateMutationState(initial: T): ImmediateMutationState<T> =
    ImmediateMutationStateImpl(initial)

@Composable
fun <T> rememberImmediateMutationState(initial: T): ImmediateMutationState<T> =
    remember { ImmediateMutationState(initial) }

@Composable
fun <T : Any> SharedInitiatingImmediateMutation(
    key: String,
    model: T,
    content: @Composable ImmediateMutationContentScope<T>.(T) -> Unit
) {
    val context = LocalSharedImmediateMutation.current
    val entry = remember(context, model) {
        context.entries.getOrPut(key) {
            SharedImmediateMutationEntry(model)
        }
    }
    val state: ImmediateMutationState<T> = remember(entry) {
        entry.getStateOfTypeOrThrow<T?>().let {
            if (it.model == null) {
                (it as ImmediateMutationStateImpl<T?>).model =model
            }
            @Suppress("UNCHECKED_CAST")
            it as ImmediateMutationState<T>
        }
    }
    entry.HoldingEffect(key)
    ImmediateMutation(
        state = state,
        model = model,
        transactionCoroutineScope = context.coroutine,
        content = content
    )
}

@Composable
inline fun <reified T : Any> SharedImmediateMutation(
    key: String,
    model: T,
    noinline content: @Composable ImmediateMutationContentScope<T>.(T) -> Unit
) {
    val context = LocalSharedImmediateMutation.current
    val entry by remember(context) {
        derivedStateOf {
            context.entries.getOrPut(key) { SharedImmediateMutationEntry(null) }
        }
    }
    val state: ImmediateMutationState<T> by remember(entry) {
        derivedStateOf {
            entry.getStateOfTypeOrThrow<T?>().let {
                if (it.model != null) {
                    @Suppress("UNCHECKED_CAST")
                    it as ImmediateMutationState<T>
                } else {
                    ImmediateMutationState(model)
                }
            }
        }
    }
    entry.HoldingEffect(key)
    ImmediateMutation(
        state = state,
        model = model,
        transactionCoroutineScope = context.coroutine,
        content = content
    )
}
