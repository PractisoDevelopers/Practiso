package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.ErrorMessage
import com.zhufucdev.practiso.datamodel.ErrorModel
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.error_at_n_para
import resources.failed_to_copy_resource_x_for_quiz_y_para
import resources.failed_to_create_interpreter_para
import resources.fei_init_span
import resources.fei_resource_span
import resources.internal_message_n_para
import resources.invalid_file_format_para
import resources.library_intent_model_span
import resources.no_details_reported_para
import resources.resource_x_was_not_found_at_y_para
import resources.unknown_span
import resources.untracked_error_para

@Composable
fun AppScope.localizedName(): String =
    when (this) {
        AppScope.Unknown -> stringResource(Res.string.unknown_span)
        AppScope.LibraryIntentModel -> stringResource(Res.string.library_intent_model_span)
        AppScope.FeiInitialization -> stringResource(Res.string.fei_init_span)
        AppScope.FeiResource -> stringResource(Res.string.fei_resource_span)
    }

@Composable
fun ErrorModel.stringTitle(): String =
    when (scope) {
        AppScope.Unknown ->
            when {
                cause != null -> cause!!::class.let { it.simpleName ?: it.qualifiedName }
                    ?: stringResource(Res.string.untracked_error_para)

                else -> stringResource(Res.string.untracked_error_para)
            }

        else -> stringResource(
            Res.string.error_at_n_para,
            scope.localizedName()
        )
    }

@Composable
fun ErrorMessage.localizedString(): String =
    when (this) {
        is ErrorMessage.Localized ->
            when (val res = resource) {
                is StringResource -> stringResource(
                    res,
                    *args.toTypedArray()
                )

                is PluralStringResource -> pluralStringResource(
                    res,
                    args.first() as Int,
                    *args.drop(1).toTypedArray()
                )

                else -> error("Unsupported localization type: ${res::class.simpleName}")
            }

        is ErrorMessage.Raw -> content

        is ErrorMessage.InvalidFileFormat -> stringResource(Res.string.invalid_file_format_para)

        is ErrorMessage.CopyResource -> stringResource(
            Res.string.failed_to_copy_resource_x_for_quiz_y_para,
            requester,
            archive
        )

        ErrorMessage.IncompatibleModel -> stringResource(
            Res.string.failed_to_create_interpreter_para
        )

        is ErrorMessage.ResourceNotFound -> stringResource(
            Res.string.resource_x_was_not_found_at_y_para,
            name, location
        )
    }

@Composable
fun ErrorModel.stringContent(): String = buildString {
    message?.let {
        appendLine(it.localizedString())
    }

    cause?.message?.let { message ->
        cause?.let { e ->
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
    } else if (isEmpty()) {
        append(stringResource(Res.string.no_details_reported_para))
    }
}
