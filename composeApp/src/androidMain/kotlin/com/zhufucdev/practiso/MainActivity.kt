package com.zhufucdev.practiso

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.zhufucdev.practiso.ui.PractisoTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            contextChan.send(this@MainActivity)
        }

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                App()
            }
        }
    }

    companion object {
        val contextChan = Channel<Context>()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}