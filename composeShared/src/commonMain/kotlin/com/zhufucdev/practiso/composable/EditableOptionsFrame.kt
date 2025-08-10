package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.image_option_para
import resources.new_option_para
import resources.new_options_frame_para
import resources.remove_from_keys_span
import resources.remove_para
import resources.set_as_key_span
import resources.text_option_para
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableOptionsFrame(
    value: Frame.Options,
    onValueChange: (Frame.Options) -> Unit,
    onDelete: () -> Unit,
    imageCache: BitmapRepository = remember { BitmapRepository() },
    modifier: Modifier = Modifier,
) {
    var frame by remember(value) { mutableStateOf(value.optionsFrame) }
    val options = remember(value) { value.frames.toMutableStateList() }
    var editingName by remember { mutableStateOf(false) }
    var appendingMenu by remember { mutableStateOf(false) }
    var masterMenu by remember { mutableStateOf(false) }

    val coroutine = rememberCoroutineScope()
    var lastFlush by remember { mutableStateOf(Clock.System.now()) }
    fun notifyValueChangeWithLocalBufferDebounced() {
        coroutine.launch {
            val flush = Clock.System.now()
            lastFlush = flush
            delay(100)
            if (lastFlush == flush) {
                onValueChange(Frame.Options(frame, options.map(KeyedPrioritizedFrame::copy)))
            }
        }
    }

    OptionsFrameSkeleton(
        modifier = modifier then if (masterMenu) Modifier.stroker() else Modifier,
        label = {
            AnimatedContent(editingName) { showTextField ->
                if (!showTextField) {
                    GlowingSurface(
                        onClick = { editingName = true },
                        onSecondaryClick = { masterMenu = true },
                        glow = frame.name.isNullOrEmpty()
                    ) {
                        Text(
                            frame.name?.takeIf(String::isNotEmpty)
                                ?: stringResource(Res.string.new_options_frame_para)
                        )
                        DropdownMenu(
                            expanded = masterMenu,
                            onDismissRequest = { masterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.remove_para)) },
                                onClick = onDelete,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                } else {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(true) {
                        focusRequester.requestFocus()
                    }

                    TextField(
                        value = frame.name ?: "",
                        onValueChange = {
                            frame = frame.copy(name = it.takeIf(String::isNotEmpty))
                        },
                        placeholder = {
                            Text(stringResource(Res.string.new_options_frame_para))
                        },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { editingName = false }
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null)
                            }
                        },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
            }
        },
        content = {
            options.forEachIndexed { index, option ->
                OptionSkeleton(
                    leading = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            state = rememberTooltipState(),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        stringResource(
                                            if (option.isKey) {
                                                Res.string.remove_from_keys_span
                                            } else {
                                                Res.string.set_as_key_span
                                            }
                                        )
                                    )
                                }
                            }
                        ) {
                            Checkbox(
                                checked = option.isKey,
                                onCheckedChange = {
                                    options[index] = option.copy(isKey = it)
                                    notifyValueChangeWithLocalBufferDebounced()
                                }
                            )
                        }
                    },
                    content = {
                        when (val frame = option.frame) {
                            is Frame.Image -> EditableImageFrame(
                                value = frame,
                                onValueChange = {
                                    options[index] = option.copy(frame = it)
                                    notifyValueChangeWithLocalBufferDebounced()
                                },
                                onDelete = {
                                    options.removeAt(index)
                                    notifyValueChangeWithLocalBufferDebounced()
                                },
                                cache = imageCache
                            )

                            is Frame.Text -> EditableTextFrame(
                                value = frame,
                                onValueChange = {
                                    options[index] = option.copy(frame = it)
                                    notifyValueChangeWithLocalBufferDebounced()
                                },
                                onDelete = {
                                    options.removeAt(index)
                                    notifyValueChangeWithLocalBufferDebounced()
                                }
                            )

                            else -> throw UnsupportedOperationException("${option.frame::class.simpleName} is not supported")
                        }
                    }
                )
            }

            Box {
                OptionSkeleton(
                    leading = {
                        Checkbox(
                            checked = false,
                            enabled = false,
                            onCheckedChange = {}
                        )
                    },
                    content = {
                        GlowingSurface(
                            onClick = { appendingMenu = true }
                        ) {
                            Text(stringResource(Res.string.new_option_para))
                        }
                    }
                )
                DropdownMenu(
                    expanded = appendingMenu,
                    onDismissRequest = { appendingMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.text_option_para)) },
                        onClick = {
                            options.add(
                                KeyedPrioritizedFrame(
                                    frame = Frame.Text(),
                                    priority = 0,
                                    isKey = false
                                )
                            )
                            appendingMenu = false
                            notifyValueChangeWithLocalBufferDebounced()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.image_option_para)) },
                        onClick = {
                            options.add(
                                KeyedPrioritizedFrame(
                                    frame = Frame.Image(),
                                    priority = 0,
                                    isKey = false
                                )
                            )
                            appendingMenu = false
                            notifyValueChangeWithLocalBufferDebounced()
                        }
                    )
                }
            }
        }
    )
}

