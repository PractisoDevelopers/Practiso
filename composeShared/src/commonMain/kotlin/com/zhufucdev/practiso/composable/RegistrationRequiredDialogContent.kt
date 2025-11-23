package com.zhufucdev.practiso.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.serialization.Serializable
import opacity.client.BonjourResponse
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.device_name_para
import resources.owner_name_para

@Composable
fun RegistrationRequiredDialogContent(
    serverInfo: BonjourResponse?,
    state: RegistrationRequiredState,
    onStateChanged: (RegistrationRequiredState) -> Unit,
) {
    LengthConstrainedTextField(
        modifier = Modifier.fillMaxWidth(),
        state = state.ownerName,
        onStateChange = { onStateChanged(state.copy(ownerName = it)) },
        label = { Text(stringResource(Res.string.owner_name_para)) },
        maxLength = serverInfo?.maxNameLength?.value
    )
    Spacer(Modifier.height(PaddingSmall))

    LengthConstrainedTextField(
        modifier = Modifier.fillMaxWidth(),
        state = state.deviceName,
        onStateChange = { onStateChanged(state.copy(deviceName = it)) },
        label = { Text(stringResource(Res.string.device_name_para)) },
        maxLength = serverInfo?.maxNameLength?.value
    )
}

@Serializable
data class RegistrationRequiredState(
    val ownerName: LengthConstrainedTextFieldBuffer,
    val deviceName: LengthConstrainedTextFieldBuffer
) {
    constructor(initialOwnerName: String, initialDeviceName: String = initialOwnerName) : this(
        LengthConstrainedTextFieldBuffer(initialOwnerName),
        LengthConstrainedTextFieldBuffer(initialDeviceName)
    )
}
