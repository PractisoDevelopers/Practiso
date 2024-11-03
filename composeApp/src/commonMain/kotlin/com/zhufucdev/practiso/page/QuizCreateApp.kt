package com.zhufucdev.practiso.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.navigate_up_para
import practiso.composeapp.generated.resources.new_question_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreateApp() {
    val coroutine = rememberCoroutineScope()
    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text(stringResource(Res.string.new_question_para)) },
            navigationIcon = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
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
        )
    }) {
        Box(Modifier.padding(it)) {
            Text("Hello from quiz creation app!")
        }
    }
}