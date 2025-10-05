package com.zhufucdev.practiso

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.composable.NotificationRationaleDialog
import com.zhufucdev.practiso.composable.PermissionAction
import com.zhufucdev.practiso.composable.PermissionRationaleState
import com.zhufucdev.practiso.style.PractisoTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : NavigatorComponentActivity(), ForActivityResultLaunchable {
    private val notificationDialog =
        MutableStateFlow<PermissionRationaleState>(PermissionRationaleState.Hidden)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PractisoTheme {
                PractisoApp(navController = rememberNavController())

                val npdState by notificationDialog.collectAsState()
                NotificationRationaleDialog(npdState, onDismissRequest = {
                    notificationDialog.tryEmit(PermissionRationaleState.Hidden)
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

    private val permissionRequestLauncher by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                lifecycleScope.launch {
                    @SuppressLint("NewApi")
                    requireNotificationPermissionRationale()
                }
            }
        }
    }

    private val activityResultChannel = Channel<ActivityResult>()
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activityResultChannel.trySend(it)
        }

    override suspend fun startActivityForResult(intent: Intent): ActivityResult {
        activityResultLauncher.launch(intent)
        return activityResultChannel.receive()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requireNotificationPermission() {
        permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun requireNotificationPermissionRationale() {
        val requestChannel = Channel<PermissionAction>()
        notificationDialog.emit(PermissionRationaleState.Request(requestChannel))

        when (requestChannel.receive()) {
            PermissionAction.Dismiss -> {}
            PermissionAction.Grant -> {
                notificationDialog.tryEmit(PermissionRationaleState.Hidden)
                requireNotificationPermission()
            }
        }
    }
}
