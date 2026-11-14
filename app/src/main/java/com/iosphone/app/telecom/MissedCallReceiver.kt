package com.iosphone.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager

class MissedCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                when (intent.action) {
                    TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION -> MissedCalls.refresh(
                        context,
                        intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER),
                        intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0),
                    )
                    "android.telecom.action.CLEAR_MISSED_CALLS" -> MissedCalls.clear(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
