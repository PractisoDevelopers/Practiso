package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.zhufucdev.practiso.style.PaddingNormal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.roundToInt

@Composable
fun AsyncAutocompleteTextField(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    candidates: Flow<List<String>> = emptyFlow(),
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var showSuggestions by remember { mutableStateOf(false) }

    val options by candidates.collectAsState(emptyList())
    val filtered by remember(options, state) {
        derivedStateOf {
            val t = state.unselectedText()
            if (t.isNotBlank()) {
                options.filter { it.startsWith(t) }
            } else {
                emptyList()
            }
        }
    }

    fun getFirstCandidate(source: CharSequence): String? =
        options.firstOrNull { it.startsWith(source) }

    Box(modifier) {
        BasicTextField(
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart).onPlaced {
                bounds = it.boundsInParent()
            },
            state = state,
            lineLimits = lineLimits,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            inputTransformation = {
                val candidate = getFirstCandidate(asCharSequence())
                println(asCharSequence())
                println(selection.start)
                if (candidate != null) {
                    val len = length
                    val sel = selection
                    if (sel.collapsed && sel.start == len &&
                        (len >= originalText.length || !originalText.startsWith(asCharSequence()))
                    ) {
                        insert(len, candidate.substring(len))
                        selection = TextRange(len, length)
                    }
                }
            },
        )
        AnimatedVisibility(
            filtered.size > 1 && !showSuggestions,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = {
                    showSuggestions = true
                },
            ) {
                Text(
                    "+${filtered.size - 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
        if (showSuggestions) {
            Popup(
                onDismissRequest = { showSuggestions = false },
                offset = bounds.bottomLeft.let {
                    IntOffset(
                        it.x.roundToInt(),
                        it.y.roundToInt()
                    )
                }) {
                Card(
                    modifier = Modifier.widthIn(max = with(LocalDensity.current) { bounds.width.toDp() })
                ) {
                    LazyColumn {
                        items(filtered, { it }) { text ->
                            Surface(
                                onClick = {
                                    state.setTextAndPlaceCursorAtEnd(text)
                                    showSuggestions = false
                                },
                                modifier = Modifier.fillParentMaxWidth()
                            ) {
                                Text(text, modifier = Modifier.padding(PaddingNormal))
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun TextFieldState.unselectedText(): String =
    text.substring(0 until selection.start) + text.substring(selection.end)