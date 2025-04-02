package com.zhufucdev.practiso.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.navigate_up_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigateUpButton() {
    val coroutine = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(stringResource(Res.string.navigate_up_para)) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = {
                coroutine.launch {
                    Navigator.navigate(Navigation.Backward)
                }
            },
        ) {
            Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = stringResource(Res.string.navigate_up_para)
            )
        }
    }
}

