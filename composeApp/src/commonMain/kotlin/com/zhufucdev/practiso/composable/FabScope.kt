package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

interface FabScope {
    @Composable
    fun ClaimScope(content: @Composable FabClaimScope.() -> Unit)
    fun clearFabClaim()
    val floatingActionButton: (@Composable () -> Unit)?
}

@Composable
fun FabScope(content: @Composable FabScope.() -> Unit) {
    var fab by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val scope = object : FabScope {
        @Composable
        override fun ClaimScope(content: @Composable FabClaimScope.() -> Unit) {
            content(object : FabClaimScope {
                override fun floatingActionButton(content: (@Composable () -> Unit)?) {
                    fab = content
                }
            })
        }

        override fun clearFabClaim() {
            fab = null
        }

        override val floatingActionButton: (@Composable () -> Unit)? get() = fab
    }

    content(scope)
}

interface FabClaimScope {
    fun floatingActionButton(content: (@Composable () -> Unit)?)
}