package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.app_name
import practiso.composeapp.generated.resources.error_at_n_para
import practiso.composeapp.generated.resources.internal_message_n_para
import practiso.composeapp.generated.resources.library_intent_model_span
import practiso.composeapp.generated.resources.untracked_error_para

data class ErrorModel(
    val scope: AppScope,
    val exception: Exception? = null,
    val message: ErrorMessage? = null,
) {
    @Composable
    fun stringTitle(): String =
        if (scope == AppScope.Unknown) {
            when {
                exception != null -> exception::class.let { it.simpleName ?: it.qualifiedName }
                    ?: stringResource(Res.string.untracked_error_para)

                else -> stringResource(Res.string.untracked_error_para)
            }
        } else {
            stringResource(Res.string.error_at_n_para, stringResource(scope.localization))
        }

    @Composable
    fun stringContent(): String = buildString {
        if (message != null) {
            append(message.getContent())
            append('\n')
        }

        exception?.message?.let { message ->
            exception.let { e ->
                append(
                    stringResource(
                        Res.string.internal_message_n_para,
                    (e::class.simpleName ?: e::class.qualifiedName)?.let { name ->
                        "$name:$message"
                    } ?: message
                ))
            }
        }

        if (last() == '\n') {
            deleteAt(lastIndex)
        }
    }
}

sealed interface ErrorMessage {
    @Composable
    fun getContent(): String

    data class Raw(val content: String) : ErrorMessage {
        @Composable
        override fun getContent(): String = content
    }

    data class Localized(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : ErrorMessage {
        @Composable
        override fun getContent(): String = stringResource(resource, *args.toTypedArray())
    }
}

enum class AppScope(val localization: StringResource) {
    Unknown(Res.string.app_name),
    LibraryIntentModel(Res.string.library_intent_model_span),
}

