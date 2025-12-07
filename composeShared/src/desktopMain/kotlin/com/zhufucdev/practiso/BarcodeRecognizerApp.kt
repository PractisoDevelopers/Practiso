package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.BarcodeOverlay
import com.zhufucdev.practiso.composable.BarcodeOverlayState
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.datamodel.AppException
import com.zhufucdev.practiso.datamodel.times
import com.zhufucdev.practiso.platform.DesktopNavigator
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.service.communityServerEndpoint
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.BarcodeRecognizerState
import com.zhufucdev.practiso.viewmodel.BarcodeRecognizerViewModel
import com.zhufucdev.practiso.viewmodel.localizedString
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_qrcode_scan
import resources.failed_to_load_image_para
import resources.file_image_plus_outline
import resources.retry_para
import resources.scan_barcodes_to_continue_para
import resources.select_from_files_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarcodeRecognizerApp(viewModel: BarcodeRecognizerViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.padding(horizontal = PaddingNormal)
                    ) {
                        NavigateUpButton(modifier = Modifier.size(36.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { insets ->
        val state by viewModel.state.collectAsState()
        AnimatedContent(state, modifier = Modifier.padding(insets)) { state ->
            when (state) {
                BarcodeRecognizerState.Empty -> StarterPage(viewModel)
                is BarcodeRecognizerState.Error -> ErrorPage(
                    state,
                    onRetry = { viewModel.event.reset.trySend(Unit) }
                )

                is BarcodeRecognizerState.Loaded -> LoadedPage(state)

                BarcodeRecognizerState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterPage(viewModel: BarcodeRecognizerViewModel) {
    val selectFromFiles = stringResource(Res.string.select_from_files_title)

    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        title = selectFromFiles,
    ) { file ->
        if (file == null) {
            return@rememberFilePickerLauncher
        }
        viewModel.event.loadImageFile.trySend(file.file)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingNormal, Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(Res.drawable.baseline_qrcode_scan),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Text(stringResource(Res.string.scan_barcodes_to_continue_para))
        Button(
            onClick = filePicker::launch
        ) {
            Icon(
                painter = painterResource(Res.drawable.file_image_plus_outline),
                contentDescription = null
            )
            Text(selectFromFiles)
        }
    }
}

@Composable
private fun LoadedPage(model: BarcodeRecognizerState.Loaded) {
    val serverEndpoint by AppSettings.communityServerEndpoint.collectAsState()
    var imageViewSize by remember { mutableStateOf(IntSize.Zero) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Box {
            Image(
                bitmap = model.image,
                contentDescription = null,
                modifier = Modifier.onGloballyPositioned {
                    imageViewSize = it.size
                }
            )
            BarcodeOverlay(
                modifier = Modifier.matchParentSize(),
                barcodes = model.barcodes.map {
                    it.copy(cornerPoints = it.cornerPoints.map { pointPx -> pointPx * (imageViewSize.width.toFloat() / model.image.width) })
                },
                state = remember(serverEndpoint) { BarcodeOverlayState(serverEndpoint) },
                onClickBarcode = { barcode ->
                    DesktopNavigator.setResult(barcode.value)
                    MainScope().launch {
                        DesktopNavigator.navigate(Navigation.Backward)
                    }
                }
            )
        }
    }
}

@Composable
private fun ErrorPage(model: BarcodeRecognizerState.Error, onRetry: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingNormal)
        ) {
            Text(
                stringResource(Res.string.failed_to_load_image_para),
                style = MaterialTheme.typography.titleLarge
            )
            (model.exception as? AppException
                ?: AppException.Generic(model.exception)).appMessage?.localizedString()
                ?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry_para))
            }
        }
    }
}