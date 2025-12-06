package com.zhufucdev.practiso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.zhufucdev.practiso.composable.BarcodeOverlay
import com.zhufucdev.practiso.composable.BarcodeOverlayState
import com.zhufucdev.practiso.composable.CameraView
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.composable.rememberCameraController
import com.zhufucdev.practiso.helper.debounced
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.service.communityServerEndpoint
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PractisoTheme
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
class QrCodeScannerActivity : NavigatorComponentActivity<String>(AppDestination.QrCodeScanner) {
    private var cameraPermissionsGranted = MutableStateFlow(false)
    val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            cameraPermissionsGranted.value = it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateCameraPermissionsGranted()
        if (!cameraPermissionsGranted.value) {
            permissionRequestLauncher.launch(Manifest.permission.CAMERA)
        }

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
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
                ) { innerPadding ->
                    val hasPermission by cameraPermissionsGranted.collectAsState()
                    if (hasPermission) {
                        val analyzer = rememberMlKitAnalyzer(
                            options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )
                        val controller = rememberCameraController(
                            analyzer = analyzer,
                            analysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
                        )
                        Box(Modifier.fillMaxSize()) {
                            CameraView(
                                modifier = Modifier.matchParentSize(),
                                controller = controller
                            )
                            val barcodesDebouncedFlow = remember(analyzer.recognizedBarcodes) {
                                analyzer.recognizedBarcodes.debounced()
                            }
                            val barcodes by barcodesDebouncedFlow.collectAsState(initial = emptyList())
                            val serverUrl by AppSettings.communityServerEndpoint.collectAsState(null)

                            serverUrl?.let { serverUrl ->
                                BarcodeOverlay(
                                    modifier = Modifier.matchParentSize(),
                                    barcodes = barcodes,
                                    state = remember { BarcodeOverlayState(serverUrl) },
                                    onClickBarcode = { barcode ->
                                        setResult(barcode.value)
                                        finish()
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(PaddingNormal)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_qrcode_scan),
                                    contentDescription = stringResource(R.string.title_qr_code_scanner),
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    stringResource(R.string.grant_camera_permissions_to_continue_para),
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = {
                                    permissionRequestLauncher.launch(Manifest.permission.CAMERA)
                                }) {
                                    Text(stringResource(R.string.grant_para))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateCameraPermissionsGranted()
    }

    private fun updateCameraPermissionsGranted() {
        cameraPermissionsGranted.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}