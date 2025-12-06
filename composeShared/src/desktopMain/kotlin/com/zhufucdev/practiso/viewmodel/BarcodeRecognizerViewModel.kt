package com.zhufucdev.practiso.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import com.zhufucdev.practiso.datamodel.Barcode
import com.zhufucdev.practiso.datamodel.BarcodeNotFoundException
import com.zhufucdev.practiso.datamodel.BarcodeType
import com.zhufucdev.practiso.datamodel.IntFlagSet
import com.zhufucdev.practiso.datamodel.intFlagSetOf
import com.zhufucdev.practiso.helper.toBarcodeFormats
import com.zhufucdev.practiso.helper.toBarcodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.withContext
import okio.IOException
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeRecognizerViewModel : ViewModel() {
    private val _state = MutableStateFlow<BarcodeRecognizerState>(BarcodeRecognizerState.Empty)
    private val barcodeReader = GenericMultipleBarcodeReader(MultiFormatReader())
    private var allowedBarcodeTypes = intFlagSetOf(BarcodeType.ALL)
    val state: StateFlow<BarcodeRecognizerState> = _state
    val event = Events()

    fun load(allowedBarcodeTypes: IntFlagSet<BarcodeType>) {
        this.allowedBarcodeTypes = allowedBarcodeTypes
        _state.value = BarcodeRecognizerState.Empty
    }

    init {
        viewModelScope.launch {
            whileSelect {
                event.loadImageFile.onReceive { file ->
                    _state.value = BarcodeRecognizerState.Loading

                    val image = withContext(Dispatchers.IO) {
                        try {
                            ImageIO.read(file)
                        } catch (e: IOException) {
                            _state.value = BarcodeRecognizerState.Error(e)
                            null
                        }
                    }
                    if (image == null) {
                        return@onReceive true
                    }
                    val hints =
                        mapOf(DecodeHintType.POSSIBLE_FORMATS to allowedBarcodeTypes.toBarcodeFormats())
                    val decodeResult = try {
                        barcodeReader.decodeMultiple(
                            BinaryBitmap(
                                HybridBinarizer(
                                    BufferedImageLuminanceSource(
                                        image
                                    )
                                )
                            ),
                            hints
                        )
                    } catch (_: NotFoundException) {
                        try {
                            barcodeReader.decodeMultiple(
                                BinaryBitmap(
                                    HybridBinarizer(
                                        BufferedImageLuminanceSource(
                                            image
                                        ).invert()
                                    )
                                ),
                                hints
                            )
                        } catch (_: NotFoundException) {
                            _state.value = BarcodeRecognizerState.Error(BarcodeNotFoundException())
                            return@onReceive true
                        }
                    }

                    _state.value = BarcodeRecognizerState.Loaded(
                        image = image.toComposeImageBitmap(),
                        barcodes = decodeResult.toBarcodes(),
                    )
                    true
                }

                event.reset.onReceive {
                    _state.value = BarcodeRecognizerState.Empty
                    true
                }
            }
        }
    }

    data class Events(
        val loadImageFile: Channel<File> = Channel(),
        val reset: Channel<Unit> = Channel(),
    )
}

sealed class BarcodeRecognizerState {
    object Empty : BarcodeRecognizerState()
    object Loading : BarcodeRecognizerState()
    data class Loaded(val image: ImageBitmap, val barcodes: List<Barcode>) :
        BarcodeRecognizerState()

    data class Error(val exception: Exception) : BarcodeRecognizerState()
}