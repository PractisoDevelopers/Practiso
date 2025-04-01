package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.style.PractisoTheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                PractisoApp(navController = rememberNavController())
            }
        }
    }
}
