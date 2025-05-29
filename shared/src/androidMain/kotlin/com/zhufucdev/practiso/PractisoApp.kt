package com.zhufucdev.practiso

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

abstract class PractisoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createFeiNotificationChannel()

        instance = this
    }

    private fun createFeiNotificationChannel() {
        val manager =
            getSystemService<NotificationManager>() ?: error("Notification Manager is unavailable.")

        val name = getString(R.string.fei_title)
        val descriptionText =
            getString(R.string.this_channel_contains_notifications_when_running_fei_para)
        manager.createNotificationChannel(
            NotificationChannel(
                FeiForegroundService.CHANNEL_ID, name,
                NotificationManager.IMPORTANCE_NONE
            ).apply { description = descriptionText }
        )
    }

    companion object {
        var instance: PractisoApp? = null
    }
}