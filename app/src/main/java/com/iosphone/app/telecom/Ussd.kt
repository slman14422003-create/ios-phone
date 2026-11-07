package com.iosphone.app.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

object Ussd {
    fun allowed(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun run(context: Context, code: String, subId: Int, onResult: (Boolean, String) -> Unit) {
        if (!allowed(context)) {
            onResult(false, "")
            return
        }
        val started = runCatching {
            val base = context.getSystemService(TelephonyManager::class.java)
            val tm = if (subId >= 0) base.createForSubscriptionId(subId) else base
            @Suppress("MissingPermission")
            tm.sendUssdRequest(
                code,
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                        onResult(true, response.toString())
                    }

                    override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                        onResult(false, "")
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }.isSuccess
        if (!started) onResult(false, "")
    }
}
