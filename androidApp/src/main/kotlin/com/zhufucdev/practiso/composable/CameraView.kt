package com.zhufucdev.practiso.composable

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executor

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    controller: CameraController? = null
) {
    val context = LocalContext.current
    val preview = remember { Preview.Builder().build() }
    val previewView = remember(preview, context) { PreviewView(context) }
    LaunchedEffect(previewView) {
        previewView.controller = controller
        preview.surfaceProvider = previewView.surfaceProvider
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
fun Context.rememberCameraController(
    analysisBackpressureStrategy: Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
    analysisResolutionSelector: ResolutionSelector? = null,
    analyzer: ImageAnalysis.Analyzer? = null,
    analyzerExecutor: Executor? = null
): CameraController {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val value = remember { LifecycleCameraController(this) }
    DisposableEffect(lifecycleOwner) {
        value.apply {
            bindToLifecycle(lifecycleOwner)
        }
        onDispose {
            value.unbind()
        }
    }
    LaunchedEffect(
        analysisBackpressureStrategy,
        analyzer,
        analyzerExecutor,
        analysisResolutionSelector
    ) {
        value.apply {
            imageAnalysisBackpressureStrategy = analysisBackpressureStrategy
            imageAnalysisResolutionSelector = analysisResolutionSelector
            if (analyzer != null) {
                setImageAnalysisAnalyzer(
                    analyzerExecutor ?: ContextCompat.getMainExecutor(context),
                    analyzer
                )
            }
        }
    }
    return value
}