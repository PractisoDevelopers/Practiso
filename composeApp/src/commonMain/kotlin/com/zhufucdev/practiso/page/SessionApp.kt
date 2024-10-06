package com.zhufucdev.practiso.page

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.composition.globalViewModel
import com.zhufucdev.practiso.viewmodel.SessionViewModel

@Composable
fun SessionApp(sessionViewModel: SessionViewModel = globalViewModel()) {
    Text("Session App")
}