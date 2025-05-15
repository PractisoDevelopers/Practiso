package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.zhufucdev.practiso.viewmodel.ImportViewModel

/**
 * [ImportViewModel] is shared because it holds state of current import
 * and the mutex to the next one.
 */
val LocalSharedImportViewModel = compositionLocalOf<ImportViewModel?> { null }

@Composable
fun currentSharedImportViewModel(): ImportViewModel {
    return LocalSharedImportViewModel.current ?: error("No Import View Model found in current context.")
}
