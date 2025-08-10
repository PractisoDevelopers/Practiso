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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.stringContent
import com.zhufucdev.practiso.viewmodel.stringTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_rhombus_outline
import resources.dismiss_para
import resources.retry_para

@Composable
fun AppExceptionAlert(
    modifier: Modifier = Modifier,
    model: Exception,
    icon: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (() -> Unit)? = null,
    additionalText: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val model by remember {
        derivedStateOf {
            if (model is AppException) {
                model
            } else {
                object : Exception(model), AppException {
                    override val scope: AppScope
                        get() = AppScope.Unknown
                    override val appMessage: AppMessage?
                        get() = AppMessage.GenericFailure
                }
            }
        }
    }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            if (onConfirmRequest != null) {
                Button(
                    onClick = onConfirmRequest
                ) {
                    Text(stringResource(Res.string.retry_para))
                }
            } else {
                OutlinedButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(Res.string.dismiss_para))
                }
            }
        },
        dismissButton = {
            if (onConfirmRequest != null) {
                OutlinedButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(Res.string.dismiss_para))
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
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall)) {
                Text(model.stringContent())
                additionalText?.invoke(this)
            }
        }
    )
}