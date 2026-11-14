package com.iosphone.app.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formats {
    private fun ar() = Locale.getDefault().language == "ar"

    fun callDate(ms: Long): String {
        val date = Date(ms)
        if (DateUtils.isToday(ms)) return DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = ms }
        val days = ((startOfDay(now) - startOfDay(then)) / DateUtils.DAY_IN_MILLIS).toInt()
        return when {
            days == 1 -> if (ar()) "أمس" else "Yesterday"
            days in 2..6 -> java.text.SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            else -> DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        }
    }

    fun fullDate(ms: Long): String {
        val date = Date(ms)
        val d = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
        val t = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
        return if (DateUtils.isToday(ms)) (if (ar()) "اليوم، $t" else "Today, $t") else "$d, $t"
    }

    fun duration(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val (hu, mu, su) = if (ar()) Triple("س", "د", "ث") else Triple("h", "m", "s")
        return when {
            h > 0 -> "$h$hu $m$mu"
            m > 0 -> "$m$mu $s$su"
            else -> "$s$su"
        }
    }

    fun timer(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun startOfDay(c: Calendar): Long {
        val x = c.clone() as Calendar
        x.set(Calendar.HOUR_OF_DAY, 0)
        x.set(Calendar.MINUTE, 0)
        x.set(Calendar.SECOND, 0)
        x.set(Calendar.MILLISECOND, 0)
        return x.timeInMillis
    }
}
