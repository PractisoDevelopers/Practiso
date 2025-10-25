package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.account_removed_para
import resources.community_service_span
import resources.download_executor_span
import resources.error_at_n_para
import resources.failed_to_create_interpreter_para
import resources.failed_to_process_resource_x_para
import resources.failed_to_process_resource_x_required_by_y_para
import resources.failed_to_setup_resource_x_at_y_para
import resources.failed_to_setup_resource_x_at_z_required_by_y_para
import resources.fei_init_span
import resources.fei_resource_span
import resources.http_error_para
import resources.http_transaction_error_para
import resources.insufficient_disk_space_para
import resources.internal_message_n_para
import resources.invalid_file_format_para
import resources.library_intent_model_span
import resources.network_unavailable_para
import resources.no_details_reported_para
import resources.undefined_error_para
import resources.unexpected_http_status_x_para
import resources.unknown_span
import resources.untracked_error_para

@Composable
fun AppScope.localizedName(): String =
    when (this) {
        AppScope.Unknown -> stringResource(Res.string.unknown_span)
        AppScope.LibraryIntentModel -> stringResource(Res.string.library_intent_model_span)
        AppScope.FeiInitialization -> stringResource(Res.string.fei_init_span)
        AppScope.FeiResource -> stringResource(Res.string.fei_resource_span)
        AppScope.DownloadExecutor -> stringResource(Res.string.download_executor_span)
        AppScope.CommunityService -> stringResource(Res.string.community_service_span)
    }

@Composable
fun AppException.stringTitle(): String =
    when (scope) {
        AppScope.Unknown ->
            when {
                this is Exception && cause != null -> cause!!::class.let {
                    it.simpleName ?: it.qualifiedName
                }
                    ?: stringResource(Res.string.untracked_error_para)

                else -> stringResource(Res.string.untracked_error_para)
            }

        else -> stringResource(
            Res.string.error_at_n_para,
            scope.localizedName()
        )
    }

@Composable
fun AppMessage.localizedString(): String =
    when (this) {
        is AppMessage.Raw -> content

        is AppMessage.InvalidFileFormat -> stringResource(Res.string.invalid_file_format_para)

        AppMessage.IncompatibleModel -> stringResource(
            Res.string.failed_to_create_interpreter_para
        )

        is AppMessage.ResourceError -> {
            when {
                location != null && requester != null ->
                    stringResource(
                        Res.string.failed_to_setup_resource_x_at_z_required_by_y_para,
                        resource, location!!, requester!!
                    )

                location != null ->
                    stringResource(
                        Res.string.failed_to_setup_resource_x_at_y_para,
                        resource, location!!
                    )

                requester != null ->
                    stringResource(
                        Res.string.failed_to_process_resource_x_required_by_y_para,
                        resource, requester!!
                    )

                else -> stringResource(Res.string.failed_to_process_resource_x_para, resource)
            }
        }

        is AppMessage.GenericFailure -> stringResource(Res.string.undefined_error_para)
        is AppMessage.GenericHttpFailure -> stringResource(Res.string.http_error_para)
        is AppMessage.HttpStatusFailure -> stringResource(
            Res.string.unexpected_http_status_x_para,
            statusCode
        )

        is AppMessage.HttpTransactionFailure -> stringResource(Res.string.http_transaction_error_para)
        is AppMessage.InsufficientSpace -> stringResource(Res.string.insufficient_disk_space_para)
        is AppMessage.AccountRemoved -> stringResource(Res.string.account_removed_para)
        is AppMessage.NetworkUnavailable -> stringResource(Res.string.network_unavailable_para)
    }

@Composable
fun AppException.stringContent(): String = buildString {
    appMessage?.let {
        appendLine(it.localizedString())
    }

    (this as? Exception)?.cause?.let { e ->
        if (e is AppException) {
            appendLine(e.stringContent())
        } else if (e.message != null) {
            append(
                stringResource(
                    Res.string.internal_message_n_para,
                    (e::class.simpleName ?: e::class.qualifiedName)?.let { name ->
                        "[$name] ${e.message!!}"
                    } ?: e.message!!
                ))
        }
    }

    if (last() == '\n') {
        deleteAt(lastIndex)
    } else if (isEmpty()) {
        append(stringResource(Res.string.no_details_reported_para))
    }
}
