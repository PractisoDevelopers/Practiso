package com.zhufucdev.practiso.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhufucdev.practiso.viewmodel.ArchiveSharingViewModel

@Composable
actual fun ArchiveSharingDialog(
    modifier: Modifier,
    model: ArchiveSharingViewModel,
    onDismissRequested: () -> Unit,
) {
    ArchiveSharingDialogScaffold(modifier, model, onDismissRequested) {
        exportToFileOption(model)
        uploadToCommunity(model)
    }
}