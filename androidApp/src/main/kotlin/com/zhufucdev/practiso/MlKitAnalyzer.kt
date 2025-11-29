package com.zhufucdev.practiso

import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.toBarcode
import com.zhufucdev.practiso.platform.eprintln
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class MlKitAnalyzer(options: BarcodeScannerOptions? = null) : ImageAnalysis.Analyzer,
    AutoCloseable {
    private val scanner =
        options?.let { BarcodeScanning.getClient(it) }
            ?: BarcodeScanning.getClient()
    private val coroutineScope =
        CoroutineScope(Dispatchers.IO) +
                SupervisorJob() +
                CoroutineExceptionHandler { context, throwable ->
                    eprintln("ML Kit encountered an analysis error:")
                    throwable.printStackTrace()
                }
    private val _barcodes = MutableStateFlow<List<Barcode>>(emptyList())

    companion object {
        val NORMALIZED_RECT = RectF(-1f, -1f, 1f, 1f)
    }

    private suspend fun launchAnalysis(proxy: ImageProxy) {
        val sensor2target = sensorToTargetTransformation ?: return
        val rotationDegrees = proxy.imageInfo.rotationDegrees
        val sensor2analysis = Matrix(proxy.imageInfo.sensorToBufferTransformMatrix)
        val source = RectF(0f, 0f, proxy.width.toFloat(), proxy.height.toFloat())
        val buffer = if (rotationDegrees == 90 || rotationDegrees == 270) RectF(
            0f,
            0f,
            proxy.height.toFloat(),
            proxy.width.toFloat()
        ) else source
        val source2buffer = Matrix().apply {
            setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL)
            postRotate(rotationDegrees.toFloat())
            postConcat(Matrix().apply {
                setRectToRect(NORMALIZED_RECT, buffer, Matrix.ScaleToFit.FILL)
            })
        }
        sensor2analysis.postConcat(source2buffer)

        val result = scanner.process(
            proxy.image ?: return,
            proxy.imageInfo.rotationDegrees,
            Matrix().apply {
                sensor2analysis.invert(this)
                postConcat(sensor2target)
            }
            ).await() ?: return
        _barcodes.emit(result.map { it.toBarcode() })
    }

    val recognizedBarcodes: StateFlow<List<Barcode>> get() = _barcodes

    override fun analyze(proxy: ImageProxy) {
        coroutineScope.launch {
            launchAnalysis(proxy)
            proxy.close()
        }
    }

    override fun close() {
        coroutineScope.cancel()
        scanner.close()
    }

    private var sensorToTargetTransformation: Matrix? = null
    override fun updateTransform(matrix: Matrix?) {
        if (matrix == null) {
            sensorToTargetTransformation = null
            return
        }
        sensorToTargetTransformation = Matrix(matrix)
    }

    override fun getTargetCoordinateSystem(): Int {
        return ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED;
    }
}

@Composable
fun rememberMlKitAnalyzer(options: BarcodeScannerOptions? = null): MlKitAnalyzer {
    val value = remember {
        MlKitAnalyzer(options)
    }
    DisposableEffect(value) {
        onDispose {
            value.close()
        }
    }

    return value
}