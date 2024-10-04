package com.zhufucdev.practiso

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.app_name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val topBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val platform = remember { getPlatform() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.app_name)) },
                scrollBehavior = topBarBehavior,
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).nestedScroll(topBarBehavior.nestedScrollConnection).fillMaxSize()) {
            items(100) {
                Text("Hello ${platform.name}")
            }
        }
    }
}