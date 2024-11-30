package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember

val LocalBottomUpComposable = compositionLocalOf<BottomUpComposable?> { null }

private typealias Content = @Composable () -> Unit

interface BottomUpComposable {
    fun set(key: String, content: Content)
    fun get(key: String): Content?
    fun clear(key: String, content: Content)

    @Composable
    fun compose(key: String)
}

private class BottomUpComposableImpl : BottomUpComposable {
    private val contents = mutableStateMapOf<String, List<Content>>()

    override fun set(key: String, content: Content) {
        contents[key] = contents.getOrElse(key) { emptyList() } + content
    }

    override fun get(key: String): Content? {
        return contents[key]?.lastOrNull()
    }

    override fun clear(key: String, content: Content) {
        contents[key] = contents[key]?.let {
            val index = it.indexOfFirst { c -> c == content }
            if (index >= 0) {
                it.subList(0, index) + it.subList(index + 1, it.size)
            } else {
                it
            }
        } ?: emptyList()
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
fun composeFromBottomUp(key: String, content: Content) {
    val lbuc = LocalBottomUpComposable.current!!
    DisposableEffect(key, content) {
        onDispose {
            lbuc.clear(key, content)
        }
    }
    LaunchedEffect(key, content) {
        lbuc.set(key, content)
    }
}