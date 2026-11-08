package com.iosphone.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> (CallManager.ringing() ?: CallManager.primary())?.let { CallManager.reject(it) }
            ACTION_HANGUP -> (CallManager.ongoing() ?: CallManager.primary())?.let { CallManager.hangup(it) }
            ACTION_REMIND -> {
                val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
                if (number.isNotBlank()) CallReminder.show(context, number, intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { number })
            }
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.iosphone.app.action.DECLINE"
        const val ACTION_HANGUP = "com.iosphone.app.action.HANGUP"
        const val ACTION_REMIND = "com.iosphone.app.action.REMIND"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
    }
}
