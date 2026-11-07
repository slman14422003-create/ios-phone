package com.iosphone.app.telecom

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.iosphone.app.MainActivity
import com.iosphone.app.R

object CallReminder {
    private const val CHANNEL = "call_reminders"

    fun schedule(context: Context, number: String, name: String, minutes: Int) {
        val app = context.applicationContext
        val alarm = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(app, CallActionReceiver::class.java)
            .setAction(CallActionReceiver.ACTION_REMIND)
            .putExtra(CallActionReceiver.EXTRA_NUMBER, number)
            .putExtra(CallActionReceiver.EXTRA_NAME, name)
        val code = (number.hashCode() and 0x3FFFFFFF)
        val pending = PendingIntent.getBroadcast(app, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pending)
        }
    }

    fun show(context: Context, number: String, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH))
        val open = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            .setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            context,
            (number.hashCode() and 0x3FFFFFFF),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(name)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        nm.notify(5000 + (number.hashCode() and 0xFFF), notification)
    }
}
