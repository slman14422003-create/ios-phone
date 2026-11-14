package com.iosphone.app.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager

object QuickReply {
    fun canSend(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun send(context: Context, number: String, text: String) {
        if (number.isBlank() || text.isBlank()) return
        val sent = canSend(context) && runCatching {
            val sms = context.getSystemService(SmsManager::class.java)
            val parts = sms.divideMessage(text)
            if (parts.size > 1) {
                sms.sendMultipartTextMessage(number, null, parts, null, null)
            } else {
                sms.sendTextMessage(number, null, text, null, null)
            }
        }.isSuccess
        if (!sent) {
            Messaging.open(context, number, text)
        }
    }
}
