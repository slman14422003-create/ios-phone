package com.iosphone.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = CallManager.primary() ?: return
        when (intent.action) {
            ACTION_DECLINE -> CallManager.reject(call)
            ACTION_HANGUP -> CallManager.hangup(call)
        }
    }

    companion object {
        const val ACTION_DECLINE = "com.iosphone.app.action.DECLINE"
        const val ACTION_HANGUP = "com.iosphone.app.action.HANGUP"
    }
}
