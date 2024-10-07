package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember

val LocalBottomUpComposable = compositionLocalOf<BottomUpComposable?> { null }

private typealias Content = @Composable () -> Unit

interface BottomUpComposable {
    fun set(key: String, content: Content?)
    fun get(key: String): Content?
    @Composable
    fun compose(key: String)
}

private class BottomUpComposableImpl : BottomUpComposable {
    private val contents = mutableStateMapOf<String, Content>()

    override fun set(key: String, content: Content?) {
        if (content == null) {
            contents.remove(key)
        } else {
            contents[key] = content
        }
    }

    override fun get(key: String): Content? {
        return contents[key]
    }

    @Composable
    override fun compose(key: String) {
        get(key)?.invoke()
    }
}

@Composable
fun BottomUpComposableScope(content: @Composable (BottomUpComposable) -> Unit) {
    val impl = remember { BottomUpComposableImpl() }
    content(impl)
}

@Composable
fun composeFromBottomUp(key: String, content: Content?) {
    LocalBottomUpComposable.current!!.set(key, content)
}