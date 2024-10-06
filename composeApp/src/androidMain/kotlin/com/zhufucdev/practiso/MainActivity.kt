package com.zhufucdev.practiso

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.zhufucdev.practiso.composition.LocalGlobalViewModelSoreOwner
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.DimensionViewModel
import com.zhufucdev.practiso.viewmodel.QuizViewModel
import com.zhufucdev.practiso.viewmodel.SessionViewModel
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
            CompositionLocalProvider(
                LocalGlobalViewModelSoreOwner provides this
            ) {
                PractisoTheme {
                    App()
                }
            }
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = viewModelFactory {
        val db by lazy {
            AndroidSqliteDriver(
                schema = AppDatabase.Schema,
                context = this@MainActivity,
                name = "practiso.db",
                callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                }
            ).toDatabase()
        }

        initializer {
            QuizViewModel(db, createSavedStateHandle())
        }

        initializer {
            DimensionViewModel(db, createSavedStateHandle())
        }

        initializer {
            SessionViewModel(db, createSavedStateHandle())
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

