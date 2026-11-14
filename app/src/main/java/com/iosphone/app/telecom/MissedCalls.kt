package com.iosphone.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.telecom.TelecomManager
import com.iosphone.app.MainActivity
import com.iosphone.app.R
import com.iosphone.app.util.PhoneFormat

/**
 * Missed-call notification owned by this app. Telecom sends SHOW_MISSED_CALLS_NOTIFICATION to the default dialer;
 * if nobody handles it the system posts its own notification, which opens the system dialer. Handling it here
 * keeps the tap inside this app (straight to the Missed filter).
 */
object MissedCalls {
    private const val CHANNEL = "missed_calls"
    const val NOTIFICATION_ID = 4102
    const val EXTRA_FILTER = "calls_filter"

    private class Unread(val count: Int, val number: String, val cachedName: String?)

    private fun query(context: Context): Unread? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return null
        var count = 0
        var number = ""
        var name: String? = null
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                CallLog.Calls.TYPE + "=? AND " + CallLog.Calls.IS_READ + "=0",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                CallLog.Calls.DATE + " DESC",
            )?.use { c ->
                count = c.count
                if (c.moveToFirst()) {
                    number = c.getString(0).orEmpty()
                    name = c.getString(1)?.takeIf { it.isNotBlank() }
                }
            }
        }
        return if (count > 0) Unread(count, number, name) else null
    }

    fun refresh(context: Context, fallbackNumber: String? = null, fallbackCount: Int = 0) {
        val app = context.applicationContext
        val nm = app.getSystemService(NotificationManager::class.java) ?: return
        val unread = query(app)
        val count = unread?.count ?: fallbackCount
        if (count <= 0) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        val number = unread?.number?.ifBlank { null } ?: fallbackNumber.orEmpty()
        val name = unread?.cachedName
            ?: runCatching { ContactLookup.find(app, number).name }.getOrNull()
            ?: PhoneFormat.format(number).ifEmpty { app.getString(R.string.unknown) }
        val channel = NotificationChannel(CHANNEL, app.getString(R.string.missed_channel), NotificationManager.IMPORTANCE_HIGH)
        channel.setShowBadge(true)
        nm.createNotificationChannel(channel)

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            app, 10,
            Intent(app, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setType(MainActivity.CALLS_TYPE)
                .putExtra(EXTRA_FILTER, 1)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )
        val dismissed = PendingIntent.getBroadcast(
            app, 11,
            Intent(app, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_MISSED_DISMISSED),
            flags,
        )
        val builder = Notification.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(name)
            .setContentText(if (count == 1) app.getString(R.string.missed_one) else app.getString(R.string.missed_many, count))
            .setContentIntent(open)
            .setDeleteIntent(dismissed)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setShowWhen(true)
            .setNumber(count)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
        if (number.isNotBlank()) {
            val callBack = PendingIntent.getBroadcast(
                app, 12,
                Intent(app, CallActionReceiver::class.java)
                    .setAction(CallActionReceiver.ACTION_CALL_BACK)
                    .putExtra(CallActionReceiver.EXTRA_NUMBER, number),
                flags,
            )
            val sms = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Messaging.installed(app)) sms.setPackage(Messaging.PACKAGE)
            val message = PendingIntent.getActivity(app, 13, sms, flags)
            builder.addAction(Notification.Action.Builder(null, app.getString(R.string.call_action), callBack).build())
            builder.addAction(Notification.Action.Builder(null, app.getString(R.string.send_message), message).build())
        }
        nm.notify(NOTIFICATION_ID, builder.build())
    }

    /** Marks missed calls as seen (call log + system badge) and removes the notification. */
    fun clear(context: Context) {
        val app = context.applicationContext
        app.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        runCatching {
            if (app.checkSelfPermission(android.Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                val values = ContentValues().apply {
                    put(CallLog.Calls.IS_READ, 1)
                    put(CallLog.Calls.NEW, 0)
                }
                app.contentResolver.update(
                    CallLog.Calls.CONTENT_URI,
                    values,
                    CallLog.Calls.TYPE + "=? AND " + CallLog.Calls.IS_READ + "=0",
                    arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                )
            }
        }
        runCatching { app.getSystemService(TelecomManager::class.java)?.cancelMissedCallsNotification() }
    }

    fun callBack(context: Context, number: String) {
        runCatching {
            context.getSystemService(TelecomManager::class.java)?.placeCall(Uri.fromParts("tel", number, null), null)
        }
    }
}
