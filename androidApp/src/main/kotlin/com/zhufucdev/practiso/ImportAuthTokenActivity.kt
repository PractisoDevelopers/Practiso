package com.zhufucdev.practiso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.CommunityIdentityDialog
import com.zhufucdev.practiso.service.communityServerEndpoint
import com.zhufucdev.practiso.style.PractisoTheme
import com.zhufucdev.practiso.viewmodel.CommunityIdentityViewModel

class ImportAuthTokenActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val protocol = intent?.data?.let { Protocol(it.toString()) }
        setContent {
            PractisoTheme {
                val serverEndpoint by AppSettings.communityServerEndpoint.collectAsState()
                val currentId = remember(serverEndpoint) {
                    serverEndpoint.let(AppSettings::getCommunityIdentity)
                }
                val viewModel = viewModel {
                    CommunityIdentityViewModel(
                        currentId,
                        AppSettings.communityServerEndpoint
                    )
                }
                LaunchedEffect(protocol) {
                    viewModel.importRequest.trySend((protocol?.action as? ProtocolAction.ImportAuthToken)?.token)
                }
                BasicAlertDialog(onDismissRequest = {}) {
                    CommunityIdentityDialog(
                        model = viewModel,
                        onDismissRequest = { finish() }
                    )
                }
            }
        }
    }
}
