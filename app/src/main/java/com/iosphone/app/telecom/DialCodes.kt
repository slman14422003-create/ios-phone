package com.iosphone.app.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager

object DialCodes {
    private val ussd = Regex("^[*#][0-9*#]*#$")
    private val secret = Regex("^\\*#\\*#(\\d+)#\\*#\\*$")

    fun isCode(raw: String): Boolean = raw == "*#06#" || secret.matches(raw) || ussd.matches(raw)

    fun imei(context: Context): String? = runCatching {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val list = ArrayList<String>()
        for (slot in 0..1) {
            val v = runCatching { tm.getImei(slot) }.getOrNull()
            if (!v.isNullOrBlank() && v !in list) list.add(v)
        }
        if (list.isEmpty()) null else list.joinToString("\n")
    }.getOrNull()

    fun secretCode(context: Context, raw: String): Boolean {
        val digits = secret.matchEntire(raw)?.groupValues?.get(1) ?: return false
        return runCatching {
            context.sendBroadcast(Intent("android.provider.Telephony.SECRET_CODE", Uri.parse("android_secret_code://$digits")))
        }.isSuccess
    }

    fun deviceInfo(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
