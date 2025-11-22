package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.zhufucdev.practiso.datamodel.ActionLabel
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.InteractiveException
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.stringContent
import com.zhufucdev.practiso.viewmodel.stringLabel
import com.zhufucdev.practiso.viewmodel.stringTitle
import org.jetbrains.compose.resources.painterResource
import resources.Res
import resources.baseline_rhombus_outline

@Composable
fun AppExceptionAlert(
    modifier: Modifier = Modifier,
    model: Exception,
    onDismissRequest: () -> Unit
) {
    val interactive = model as? InteractiveException
    AppExceptionAlertScaffold(
        modifier = modifier,
        model = model as? AppException ?: AppException.Generic(model),
        onDismissRequest = onDismissRequest,
        onPrimaryActionRequest = interactive?.let { ie ->
            {
                ie.sendPrimary()
                onDismissRequest()
            }
        },
        onSecondaryActionRequest = interactive?.let { ie ->
            {
                ie.sendSecondary()
                onDismissRequest()
            }
        },
        primaryActionLabel = interactive?.primaryActionLabel ?: ActionLabel.Confirm,
        secondaryActionLabel = interactive?.secondaryActionLabel ?: ActionLabel.Cancel
    ) { model ->
        Text(model.stringContent())
    }
}

@Composable
fun AppExceptionAlert(
    modifier: Modifier = Modifier,
    model: Exception,
    icon: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onRetryRequest: (() -> Unit)? = null,
    additionalText: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AppExceptionAlertScaffold(
        modifier = modifier,
        model = model as? AppException ?: AppException.Generic(model),
        icon = icon,
        onDismissRequest = onDismissRequest,
        onPrimaryActionRequest = onRetryRequest,
        onSecondaryActionRequest = onDismissRequest,
        text = { model ->
            Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                Text(model.stringContent())
                additionalText?.invoke(this)
            }
        }
    )
}

@Composable
fun AppExceptionAlertScaffold(
    modifier: Modifier = Modifier,
    model: AppException,
    icon: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onPrimaryActionRequest: (() -> Unit)? = null,
    onSecondaryActionRequest: (() -> Unit)? = null,
    primaryActionLabel: ActionLabel = ActionLabel.Confirm,
    secondaryActionLabel: ActionLabel = ActionLabel.Cancel,
    text: @Composable (AppException) -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            if (onPrimaryActionRequest != null) {
                Button(
                    onClick = onPrimaryActionRequest
                ) {
                    Text(primaryActionLabel.stringLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (onSecondaryActionRequest != null) {
                            onSecondaryActionRequest()
                        } else {
                            onDismissRequest()
                        }
                    }
                ) {
                    Text(primaryActionLabel.stringLabel())
                }
            }
        },
        dismissButton = {
            if (onPrimaryActionRequest != null) {
                OutlinedButton(
                    onClick = {
                        onSecondaryActionRequest?.invoke()
                    }
                ) {
                    Text(secondaryActionLabel.stringLabel())
                }
            }
        },
        title = {
            Text(
                model.stringTitle(),
                textAlign = TextAlign.Center
            )
        },
        icon = {
            icon?.invoke() ?: Icon(
                painterResource(Res.drawable.baseline_rhombus_outline),
                contentDescription = null
            )
        },
        text = { text(model) }
    )
}