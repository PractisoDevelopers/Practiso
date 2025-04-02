package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composition.ExtensiveSnackbarState
import com.zhufucdev.practiso.composition.SnackbarExtension
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.style.PaddingNormal
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.dismiss_para

@Composable
fun ExtensiveSnackbar(state: ExtensiveSnackbarState, data: SnackbarData) {
    var modifier: Modifier = Modifier

    val progressBar =
        state.extensions.filterFirstIsInstanceOrNull<SnackbarExtension.ProgressBar>()
    if (progressBar != null) {
        val primaryColor = MaterialTheme.colorScheme.inversePrimary
        val progress by progressBar.progress.collectAsState()
        val progressProxy by (if (progressBar.animated) animateFloatAsState(
            progress,
            animationSpec = spring()
        ) else derivedStateOf { progress })

        val width = 2.dp
        modifier = modifier.drawWithContent {
            val widthPx = width.toPx()
            drawContent()
            drawLine(
                color = primaryColor,
                start = Offset(x = 0f, y = widthPx / 2),
                end = Offset(
                    x = size.width * progressProxy,
                    y = widthPx / 2
                ),
                strokeWidth = widthPx,
            )
        }
    }

    val actionLabel = data.visuals.actionLabel
    val actionComposable: (@Composable () -> Unit)? =
        if (actionLabel != null) {
            @Composable {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                    onClick = { data.performAction() },
                    content = { Text(actionLabel) }
                )
            }
        } else {
            null
        }
    val dismissActionComposable: (@Composable () -> Unit)? =
        if (data.visuals.withDismissAction) {
            @Composable {
                IconButton(
                    onClick = { data.dismiss() },
                    content = {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.dismiss_para),
                        )
                    }
                )
            }
        } else {
            null
        }

    Box(Modifier.padding(PaddingNormal).clip(SnackbarDefaults.shape).shadow(PaddingNormal)) {
        Snackbar(
            modifier = modifier,
            shape = RectangleShape,
            action = actionComposable,
            dismissAction = dismissActionComposable,
            content = { Text(data.visuals.message) }
        )
    }
}