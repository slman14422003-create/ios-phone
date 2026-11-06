package com.iosphone.app.util

import android.telephony.PhoneNumberUtils
import java.util.Locale

object PhoneFormat {
    fun format(raw: String): String {
        if (raw.isEmpty()) return raw
        if (raw.any { it == '*' || it == '#' } && raw.length < 4) return raw
        val formatted = PhoneNumberUtils.formatNumber(raw, Locale.getDefault().country)
        return formatted ?: raw
    }

    fun key(raw: String): String = raw.filter { it.isDigit() }.takeLast(9)

    fun digits(raw: String): String = raw.filter { it.isDigit() || it == '+' }
}
