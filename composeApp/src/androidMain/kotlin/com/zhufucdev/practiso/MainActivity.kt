package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.style.PractisoTheme
import io.github.vinceglb.filekit.core.FileKit

class MainActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                PractisoApp(
                    navController = rememberNavController()
                )
            }
        }
    }
}
