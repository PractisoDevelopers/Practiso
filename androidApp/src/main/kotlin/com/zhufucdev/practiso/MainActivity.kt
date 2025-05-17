package com.zhufucdev.practiso

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.composable.NotificationPermissionDialog
import com.zhufucdev.practiso.composable.PermissionAction
import com.zhufucdev.practiso.composable.PermissionDialogState
import com.zhufucdev.practiso.style.PractisoTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : NavigatorComponentActivity() {
    private val notificationDialog =
        MutableStateFlow<PermissionDialogState>(PermissionDialogState.Hidden)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                PractisoApp(navController = rememberNavController())

                val npdState by notificationDialog.collectAsState()
                NotificationPermissionDialog(npdState, onDismissRequest = {
                    notificationDialog.tryEmit(PermissionDialogState.Hidden)
                })
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                lifecycleScope.launch {
                    requireNotificationPermissionRationale()
                }
            } else {
                requireNotificationPermission()
            }
        }
    }

    private val launcher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                lifecycleScope.launch {
                    @SuppressLint("NewApi")
                    requireNotificationPermissionRationale()
                }
            } else if (isGranted) {
                notificationDialog.tryEmit(PermissionDialogState.Hidden)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requireNotificationPermission() {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun requireNotificationPermissionRationale() {
        val requestChannel = Channel<PermissionAction>()
        notificationDialog.emit(PermissionDialogState.Request(requestChannel))

        when (requestChannel.receive()) {
            PermissionAction.Dismiss -> {}
            PermissionAction.Grant -> {
                requireNotificationPermission()
            }
        }
    }
}
