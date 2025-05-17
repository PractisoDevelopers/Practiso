package com.zhufucdev.practiso.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.zhufucdev.practiso.R
import kotlinx.coroutines.channels.SendChannel

@Composable
fun NotificationRationaleDialog(model: PermissionRationaleState, onDismissRequest: () -> Unit) {
    when (model) {
        PermissionRationaleState.Hidden -> {}
        is PermissionRationaleState.Request -> {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    Button(
                        onClick = {
                            model.action.trySend(PermissionAction.Grant)
                        }
                    ) {
                        Text(stringResource(R.string.grant_para))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            model.action.trySend(PermissionAction.Dismiss)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.dismiss_para))
                    }
                },
                title = {
                    Text(stringResource(R.string.notification_permission_denied_title))
                },
                icon = {
                    Icon(painterResource(R.drawable.ic_outline_notifications), contentDescription = null)
                },
                text = {
                    Text(stringResource(R.string.explain_why_denying_notification_is_a_bad_idea))
                }
            )
        }
    }
}

sealed class PermissionRationaleState {
    data object Hidden : PermissionRationaleState()
    data class Request(val action: SendChannel<PermissionAction>) : PermissionRationaleState()
}

sealed class PermissionAction {
    data object Grant : PermissionAction()
    data object Dismiss : PermissionAction()
}