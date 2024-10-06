package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalGlobalViewModelSoreOwner = compositionLocalOf<ViewModelStoreOwner?> { null }

@Composable
inline fun <reified T : ViewModel> globalViewModel(): T {
    return viewModel<T>(LocalGlobalViewModelSoreOwner.current ?: error("no global view model store owner"))
}
