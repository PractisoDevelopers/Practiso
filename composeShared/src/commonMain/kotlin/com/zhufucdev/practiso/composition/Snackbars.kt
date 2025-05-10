package com.zhufucdev.practiso.composition

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow

val SharedExtensiveSnackbarState = ExtensiveSnackbarState()
val LocalExtensiveSnackbarState = compositionLocalOf { SharedExtensiveSnackbarState }

@Stable
class ExtensiveSnackbarState(
    val host: SnackbarHostState = SnackbarHostState(),
    extensions: List<SnackbarExtension> = emptyList(),
) {
    private var _extensions by mutableStateOf(extensions)
    val extensions get() = _extensions

    suspend fun showSnackbar(
        message: String,
        vararg extensions: SnackbarExtension,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Long,
    ): SnackbarResult {
        _extensions = extensions.toList()
        return host.showSnackbar(message, actionLabel, withDismissAction, duration)
    }
}

sealed interface SnackbarExtension {
    data class ProgressBar(val progress: StateFlow<Float>, val animated: Boolean = true) :
        SnackbarExtension

    data class Identifier<T>(val id: T) : SnackbarExtension
}
