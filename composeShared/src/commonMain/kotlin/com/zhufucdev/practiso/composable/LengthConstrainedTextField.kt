package com.zhufucdev.practiso.composable

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

@Composable
fun LengthConstrainedTextField(
    modifier: Modifier = Modifier,
    state: LengthConstrainedTextFieldBuffer,
    onStateChange: (LengthConstrainedTextFieldBuffer) -> Unit,
    label: (@Composable () -> Unit)? = null,
    maxLength: Int? = null,
) {
    TextField(
        modifier = modifier,
        value = state.value,
        onValueChange = {
            onStateChange(LengthConstrainedTextFieldBuffer.of(it, maxLength))
        },
        label = label,
        supportingText = {
            if (maxLength != null) {
                Text("${state.value.length} / $maxLength")
            }
        },
        isError = state.isOversized
    )
}

@Immutable
@Serializable
data class LengthConstrainedTextFieldBuffer(
    val value: String,
    val isOversized: Boolean = false,
) {
    companion object {
        fun of(value: String, maxLength: Int? = null): LengthConstrainedTextFieldBuffer {
            return LengthConstrainedTextFieldBuffer(
                value = value,
                isOversized = maxLength?.let { maxLength -> maxLength < value.length } == true
            )
        }
    }
}
