package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.style.PaddingNormal
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.cancel_para
import resources.confirm_para
import resources.empty_text_para
import resources.remove_para

@Composable
fun EditableTextFrame(
    value: Frame.Text,
    onValueChange: (Frame.Text) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf(false) }
    var buffer by remember { mutableStateOf(value.textFrame.content) }

    AnimatedContent(!editing, modifier) { showTextField ->
        if (showTextField) {
            GlowingSurface(
                onClick = { editing = true },
                onSecondaryClick = { contextMenu = true },
                glow = value.textFrame.content.isEmpty()
            ) {
                TextFrameSkeleton {
                    Text(
                        value.textFrame.content.takeIf(String::isNotEmpty)
                            ?: stringResource(Res.string.empty_text_para),
                    )
                }
                DropdownMenu(expanded = contextMenu, onDismissRequest = { contextMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_para)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = onDelete
                    )
                }
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }

            Column {
                TextField(
                    value = buffer,
                    onValueChange = { buffer = it },
                    placeholder = { Text(stringResource(Res.string.empty_text_para)) },
                    modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingNormal, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        buffer = value.textFrame.content
                        editing = false
                    }) {
                        Text(stringResource(Res.string.cancel_para))
                    }
                    Button(onClick = {
                        onValueChange(value.copy(textFrame = value.textFrame.copy(content = buffer)))
                        editing = false
                    }) {
                        Text(stringResource(Res.string.confirm_para))
                    }
                }
            }
        }
    }
}

