package com.zhufucdev.practiso.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

private val DEFAULT_HEIGHT = 46.dp

expect fun Modifier.actionTextClickable(primary: () -> Unit, secondary: () -> Unit): Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionText(
    modifier: Modifier = Modifier,
    text: String,
    actions: List<TextAction>,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var selected by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Text(
            text = visualTransformation.filter(AnnotatedString(text)).text,
            style = LocalTextStyle.current.let {
                if (selected) {
                    it.copy(background = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    it
                }
            },
            modifier = Modifier.actionTextClickable(
                secondary = {
                    selected = !selected
                },
                primary = {
                    if (selected) {
                        selected = false
                    }
                }
            ),
        )
        if (selected) {
            Popup(
                onDismissRequest = { selected = false },
                alignment = Alignment.TopCenter,
                offset = IntOffset(
                    x = 0,
                    y = with(LocalDensity.current) { -(DEFAULT_HEIGHT + 8.dp).roundToPx() })
            ) {
                val cardShape = RoundedCornerShape(100)
                Card(
                    shape = cardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    Row(modifier = Modifier.heightIn(max = DEFAULT_HEIGHT)) {
                        actions.forEach { (label, action) ->
                            Surface(
                                onClick = {
                                    action()
                                    selected = false
                                },
                                color = Color.Transparent,
                            ) {
                                Box(
                                    modifier = Modifier.padding(
                                        vertical = 12.dp,
                                        horizontal = 14.dp
                                    ).fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
data class TextAction(val label: String, val action: () -> Unit)

