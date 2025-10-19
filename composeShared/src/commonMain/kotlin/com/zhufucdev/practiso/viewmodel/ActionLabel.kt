package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.ActionLabel
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.cancel_para
import resources.clear_token_para
import resources.confirm_para
import resources.retry_para

@Composable
fun ActionLabel.stringLabel(): String =
    when(this) {
        ActionLabel.Cancel -> stringResource(Res.string.cancel_para)
        ActionLabel.Confirm -> stringResource(Res.string.confirm_para)
        ActionLabel.ClearToken -> stringResource(Res.string.clear_token_para)
        ActionLabel.Retry -> stringResource(Res.string.retry_para)
    }
