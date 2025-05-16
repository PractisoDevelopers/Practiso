package com.zhufucdev.practiso.platform

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.zhufucdev.practiso.FeiForegroundService
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.TextFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun platformGetEmbeddings(
    textFrames: Set<TextFrame>,
    imageFrames: Set<ImageFrame>,
    fei: FrameEmbeddingInference,
    parallelTasks: Int,
): Flow<InferenceState> {
    if (SharedContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        val serviceIntent = Intent(SharedContext, FeiForegroundService::class.java)
        serviceIntent.putExtra(
            FeiForegroundService.ITEMS_COUNT_EXTRA,
            textFrames.size + imageFrames.size
        )

        return channelFlow {
            binderScope<FeiForegroundService.FeiBinder, Flow<InferenceState>>(
                serviceIntent,
                SharedContext
            ) {
                service.startJob(textFrames, imageFrames, fei, parallelTasks)
            }.collect {
                when (it) {
                    is InferenceState.Complete -> {
                        send(it)
                        close()
                    }
                    is InferenceState.Inferring -> trySend(it)
                }
            }
        }
    } else {
        return getEmbeddingsUnconfined(textFrames, imageFrames, fei, parallelTasks)
    }
}

suspend fun <T : IBinder, R> binderScope(
    intent: Intent,
    context: Context,
    block: suspend T.() -> R,
): R {
    val connectionScope = CoroutineScope(currentCoroutineContext())
    return suspendCancellableCoroutine { c ->
        val connection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder?,
            ) {
                val binder = service as T
                connectionScope.launch {
                    c.resume(block(binder))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                connectionScope.cancel()
                c.cancel()
            }
        }

        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        context.startForegroundService(intent)
    }
}