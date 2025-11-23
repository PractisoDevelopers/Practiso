package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PractisoTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import qrcode.QRCode

class QrCodeViewerActivity : NavigatorComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val options = navigationOptions.filterFirstIsInstanceOrNull<NavigationOption.OpenQrCode>()
        if (options == null) {
            MainScope().launch {
                Navigator.navigate(Navigation.Backward)
            }
            return
        }

        setContent {
            PractisoTheme {
                val pixelColor = MaterialTheme.colorScheme.primary.toArgb()
                val model = remember(options.stringValue) {
                    QRCode.ofRoundedSquares()
                        .withColor(pixelColor)
                        .build(options.stringValue)
                }

                QrCodeViewerApp(model, options.title)
            }
        }
    }
}