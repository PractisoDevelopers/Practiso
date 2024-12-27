package com.zhufucdev.practiso

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.datamodel.Importable
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import io.github.vinceglb.filekit.core.FileKit
import okio.source

class MainActivity : NavigatorComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)

        enableEdgeToEdge()
        setContent {
            val importer: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
            LaunchedEffect(importer) {
                if (intent.action == Intent.ACTION_VIEW) {
                    val uri = intent.data!!
                    contentResolver.openInputStream(uri)?.use {
                        val target = Importable(
                            name = uri.path?.split('/')?.lastOrNull() ?: getString(R.string.generic_file_para),
                            source = it.source()
                        )
                        importer.import(target)
                    }
                    finish()
                }
            }
            PractisoTheme {
                PractisoApp(
                    importViewModel = importer,
                    navController = rememberNavController()
                )
            }
        }
    }
}
