package com.zhufucdev.practiso.page

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.AppExceptionAlert
import com.zhufucdev.practiso.composable.ArchiveMetadataOption
import com.zhufucdev.practiso.composable.OptionItem
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.uiSharedId
import com.zhufucdev.practiso.viewmodel.CommunityDimensionViewModel
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import resources.Res
import resources.details_para
import resources.failed_to_download_archive_para

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CommunityDimensionApp(
    dimensionModel: CommunityDimensionViewModel = viewModel(factory = CommunityDimensionViewModel.Factory),
    importModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
    sharedTransition: SharedTransitionScope,
    animatedContent: AnimatedContentScope,
) {
    val snackbars = LocalExtensiveSnackbarState.current
    val navController = LocalNavController.current

    val archives by dimensionModel.archives.collectAsState(Dispatchers.IO)
    val downloadError by dimensionModel.downloadError.collectAsState()
    var alertError by remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(downloadError, snackbars) {
        if (downloadError == null) {
            return@LaunchedEffect
        }

        val action = snackbars.showSnackbar(
            message = getString(Res.string.failed_to_download_archive_para),
            actionLabel = getString(Res.string.details_para)
        )
        if (action == SnackbarResult.ActionPerformed) {
            alertError = downloadError
        }
    }

    LazyColumn {
        archives?.let { archives ->
            items(count = archives.items.size, key = { archives.items[it].id }) { index ->
                val archive = archives.items[index]
                val state by dimensionModel.getDownloadStateFlow(archive).collectAsState()
                val lifecycleScope = rememberCoroutineScope()
                OptionItem(
                    modifier = Modifier.clickable(onClick = {
                        lifecycleScope.launch {
                            navController?.navigate(
                                ArchivePreviewRouteParams(
                                    metadata = archive,
                                    selectedDimensions = listOf(dimensionModel.dimension.first())
                                )
                            )
                        }
                    }),
                    separator = index < archives.items.lastIndex || archives.isMounting
                ) {
                    ArchiveMetadataOption(
                        modifier = with(sharedTransition) {
                            Modifier.sharedElement(
                                rememberSharedContentState(archive.uiSharedId),
                                animatedContent
                            )
                        },
                        model = archive,
                        state = state,
                        onDownloadRequest = {
                            dimensionModel.archiveEvent.downloadAndImport.trySend(archive to importModel.event)
                        },
                        onCancelRequest = {
                            dimensionModel.archiveEvent.cancelDownload.trySend(archive)
                        },
                        onErrorDetailsRequest = {
                            alertError = it
                        }
                    )
                }
            }
        }

        if (archives?.isMounting != false) {
            items(count = 5) {
                OptionItem(separator = it != 4) {
                    PractisoOptionSkeleton()
                }
            }
        }
    }
    alertError?.let { error ->
        AppExceptionAlert(
            model = error,
            onDismissRequest = {
                alertError = null
            }
        )
    }
}