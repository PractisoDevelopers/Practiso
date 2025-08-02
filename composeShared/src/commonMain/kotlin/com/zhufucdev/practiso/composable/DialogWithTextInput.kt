package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingSmall

@Composable
fun DialogWithTextInput(
    icon: @Composable ColumnScope.() -> Unit,
    title: @Composable ColumnScope.() -> Unit,
    inputValue: String,
    onInputValueChange: (String) -> Unit,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    negativeButton: (@Composable () -> Unit)? = null,
    positiveButton: @Composable () -> Unit
) {
    Card {
        DialogContentSkeleton(
            modifier = Modifier.padding(PaddingBig).fillMaxWidth(),
            icon = icon,
            title = title
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputValueChange,
                label = label,
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingSmall, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                negativeButton?.invoke()
                positiveButton()
            }
        }
    }
}