package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.style.PractisoTheme

class PreferencesActivity : NavigatorComponentActivity<Unit>(AppDestination.Preferences) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                PreferencesApp()
            }
        }
    }
}