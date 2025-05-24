package com.zhufucdev.practiso

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.platform.AndroidPlatform
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.InferenceState
import com.zhufucdev.practiso.platform.getEmbeddingsUnconfined
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class FeiForegroundService : LifecycleService() {
    private val _state = MutableSharedFlow<InferenceState>()
    val state: SharedFlow<InferenceState> get() = _state
    private lateinit var notification: NotificationCompat.Builder

    @SuppressLint("MissingPermission")
    fun startJob(
        textFrames: Set<TextFrame>,
        imageFrames: Set<ImageFrame>,
        fei: FrameEmbeddingInference,
        parallelTasks: Int,
    ): Flow<InferenceState> {
        val nmc = NotificationManagerCompat.from(this)
        var totalDone = 0

        return getEmbeddingsUnconfined(textFrames, imageFrames, fei, parallelTasks)
            .flowOn(Dispatchers.Default)
            .flowWithLifecycle(lifecycle)
            .onEach { state ->
                _state.emit(state)
                when (state) {
                    is InferenceState.Complete -> {
                        stopSelf()
                        nmc.notify(NOTIFICATION_ID, notification.setProgress(0, 0, false).build())
                    }

                    is InferenceState.Inferring -> {
                        totalDone += state.done
                        nmc.notify(
                            NOTIFICATION_ID,
                            notification.setProgress(state.total, totalDone, false).build()
                        )
                    }
                }
            }
    }

    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_creation)
                .setContentTitle(getString(R.string.fei_title))
                .setContentText(
                    intent?.extras?.getInt(ITEMS_COUNT_EXTRA)
                        ?.let { resources.getQuantityString(R.plurals.inferring_n_items, it, it) }
                        ?: getString(R.string.computing_meanings_para)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AndroidPlatform.maxParallelInferences.update {
            val newCap = maxOf(it * 2 / 3, 1)
            Log.w("FeiForegroundService", "Low memory. Updating parallelism cap to $newCap")
            newCap
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationManagerCompat.from(this)
            .cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "fei_channel"
        const val NOTIFICATION_ID = 1
        const val ITEMS_COUNT_EXTRA = "items_count"
    }

    inner class FeiBinder : Binder() {
        val service get() = this@FeiForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return FeiBinder()
    }
}