package com.zhufucdev.practiso

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.ImportDialog
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import kotlinx.coroutines.flow.first
import okio.source

class ImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)
        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                val importer: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
                LaunchedEffect(importer) {
                    val uri = when (intent.action) {
                        Intent.ACTION_VIEW -> {
                            intent.data!!
                        }

                        Intent.ACTION_SEND -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                            } else {
                                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                            }
                        }

                        else -> error("Should never reach here")
                    }
                    if (uri != null) {
                        contentResolver.openInputStream(uri)?.use {
                            val target = NamedSource(
                                name = uri.path?.split('/')?.lastOrNull()
                                    ?: getString(R.string.generic_file_para),
                                source = it.source()
                            )
                            importer.event.import.send(target)
                            importer.event.importFinish.first()
                        }
                    }
                    finish()
                }

                val state by importer.state.collectAsState()
                LaunchedEffect(state) {
                    Log.d("state", state::class.simpleName.toString())
                }
                ImportDialog(state)
            }
        }
    }
}