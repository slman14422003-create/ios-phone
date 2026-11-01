package com.iosphone.app.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date

object Formats {
    fun callDate(ms: Long): String {
        val date = Date(ms)
        return if (DateUtils.isToday(ms)) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
        } else {
            DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        }
    }
}
