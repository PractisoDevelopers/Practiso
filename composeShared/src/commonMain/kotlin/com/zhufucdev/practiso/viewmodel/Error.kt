package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.ErrorMessage
import com.zhufucdev.practiso.datamodel.ErrorModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.error_at_n_para
import resources.failed_to_copy_resource_x_for_quiz_y_para
import resources.internal_message_n_para
import resources.invalid_file_format_para
import resources.library_intent_model_span
import resources.untracked_error_para

data class LocalizedErrorMessage(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
) : ErrorMessage

@Composable
fun ErrorModel.stringTitle(): String =
    when (scope) {
        AppScope.Unknown ->
            when {
                exception != null -> exception!!::class.let { it.simpleName ?: it.qualifiedName }
                    ?: stringResource(Res.string.untracked_error_para)

                else -> stringResource(Res.string.untracked_error_para)
            }

        AppScope.LibraryIntentModel -> stringResource(
            Res.string.error_at_n_para,
            stringResource(Res.string.library_intent_model_span)
        )
    }

@Composable
fun ErrorModel.stringContent(): String = buildString {
    message?.let {
        append(
            when (val message = it) {
                is LocalizedErrorMessage -> stringResource(
                    message.resource,
                    *message.args.toTypedArray()
                )

                is ErrorMessage.Raw -> message.content

                is ErrorMessage.InvalidFileFormat -> stringResource(Res.string.invalid_file_format_para)

                is ErrorMessage.CopyResource -> stringResource(
                    Res.string.failed_to_copy_resource_x_for_quiz_y_para,
                    message.requester,
                    message.archive
                )

                else -> error("Unknown ErrorMessage subtype: ${message::class.simpleName}")
            }
        )
        append('\n')
    }

    exception?.message?.let { message ->
        exception?.let { e ->
            append(
                stringResource(
                    Res.string.internal_message_n_para,
                    (e::class.simpleName ?: e::class.qualifiedName)?.let { name ->
                        "[$name] $message"
                    } ?: message
                ))
        }
    }

    if (last() == '\n') {
        deleteAt(lastIndex)
    }
}
