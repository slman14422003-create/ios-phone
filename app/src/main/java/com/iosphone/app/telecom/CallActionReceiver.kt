package com.iosphone.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE -> (CallManager.ringing() ?: CallManager.primary())?.let { CallManager.reject(it) }
            ACTION_HANGUP -> (CallManager.ongoing() ?: CallManager.primary())?.let { CallManager.hangup(it) }
            ACTION_MISSED_DISMISSED -> Thread { MissedCalls.clear(context) }.start()
            ACTION_CALL_BACK -> {
                val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
                MissedCalls.clear(context)
                if (number.isNotBlank()) MissedCalls.callBack(context, number)
            }
            ACTION_REMIND -> {
                val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
                if (number.isNotBlank()) CallReminder.show(context, number, intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { number })
            }
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.iosphone.app.action.DECLINE"
        const val ACTION_HANGUP = "com.iosphone.app.action.HANGUP"
        const val ACTION_MISSED_DISMISSED = "com.iosphone.app.action.MISSED_DISMISSED"
        const val ACTION_CALL_BACK = "com.iosphone.app.action.CALL_BACK"
        const val ACTION_REMIND = "com.iosphone.app.action.REMIND"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
    }
}
