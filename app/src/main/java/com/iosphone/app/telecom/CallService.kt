package com.iosphone.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.iosphone.app.InCallActivity
import com.iosphone.app.R
import com.iosphone.app.util.PhoneFormat

@Suppress("DEPRECATION")
class CallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
    }

    override fun onDestroy() {
        if (CallManager.service === this) CallManager.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        CallManager.add(call)
        if (call.state == Call.STATE_RINGING) showIncoming(call) else openUi()
    }

    override fun onCallRemoved(call: Call) {
        CallManager.remove(call)
        cancelIncoming()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        if (audioState != null) {
            CallManager.muted = audioState.isMuted
            CallManager.route = audioState.route
        }
    }

    fun cancelIncoming() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun openUi() {
        val intent = Intent(this, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun showIncoming(call: Call) {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.incoming_channel), NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null)
        channel.enableVibration(false)
        nm.createNotificationChannel(channel)

        val number = CallManager.info(call).number
        val name = ContactLookup.find(this, number).name ?: PhoneFormat.format(number).ifEmpty { getString(R.string.unknown) }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val uiFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, InCallActivity::class.java).addFlags(uiFlags), flags,
        )
        val answer = PendingIntent.getActivity(
            this, 1,
            Intent(this, InCallActivity::class.java).setAction(InCallActivity.ACTION_ANSWER).addFlags(uiFlags),
            flags,
        )
        val decline = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_DECLINE),
            flags,
        )
        val person = Person.Builder().setName(name).setImportant(true).build()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setStyle(Notification.CallStyle.forIncomingCall(person, decline, answer))
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "incoming_calls"
        const val NOTIFICATION_ID = 4101
    }
}
