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

    /**
     * IMEI / MEID of every slot, one labeled line each. Android 10+ only hands these to privileged apps,
     * so this returns null on most modern devices and the caller falls back to [openImeiScreen].
     */
    fun imei(context: Context): String? = runCatching {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val slots = tm.phoneCount.coerceIn(1, 2)
        val lines = ArrayList<String>()
        val seen = HashSet<String>()
        for (slot in 0 until slots) {
            val v = runCatching { tm.getImei(slot) }.getOrNull()
            if (!v.isNullOrBlank() && seen.add(v)) lines.add("IMEI ${lines.size + 1}: $v")
        }
        for (slot in 0 until slots) {
            val v = runCatching { tm.getMeid(slot) }.getOrNull()
            if (!v.isNullOrBlank() && seen.add(v)) lines.add("MEID: $v")
        }
        if (lines.isEmpty()) null else lines.joinToString("\n")
    }.getOrNull()

    /**
     * Fallback when the IMEI cannot be read: open the system screens that list it ("Status" first, which
     * shows IMEI directly), and only then the generic About-phone page.
     */
    fun openImeiScreen(context: Context) {
        val pm = context.packageManager
        // The system dialer (Samsung Phone / Google Phone) is privileged and can show the IMEI itself.
        for (pkg in listOf("com.samsung.android.dialer", "com.google.android.dialer", "com.android.dialer")) {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode("*#06#")))
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (dial.resolveActivity(pm) == null) continue
            if (runCatching { context.startActivity(dial) }.isSuccess) return
        }
        val candidates = listOf(
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$StatusActivity"),
            Intent().setClassName("com.android.settings", "com.android.settings.Settings\$ImeiInformationActivity"),
            Intent("android.settings.DEVICE_INFO_SETTINGS"),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) == null) continue
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
        deviceInfo(context)
    }

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
