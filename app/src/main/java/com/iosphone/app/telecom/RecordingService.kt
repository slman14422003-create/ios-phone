package com.iosphone.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.iosphone.app.R

class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.recording_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(getString(R.string.recording_notification))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        val ok = runCatching {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        }.isSuccess
        if (ok) CallRecorder.serviceReady() else stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "call_recording"
        const val NOTIFICATION_ID = 4102
    }
}
