package com.zhufucdev.practiso

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AuthTokenExportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
        if (text != null) {
            MainScope().launch {
                Navigator.navigate(
                    Navigation.Goto(AppDestination.QrCodeViewer),
                    NavigationOption.OpenQrCode(stringValue = text)
                )
            }
        }
        finish()
    }
}
