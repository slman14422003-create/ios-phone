package com.iosphone.app.domain

import android.provider.CallLog
import com.iosphone.app.data.CallEntry
import com.iosphone.app.util.PhoneFormat
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.exp

/** On-device call habits: who you usually call around this time, unreturned missed calls, likely spam. */
object CallIntel {
    class Pick(val number: String, val score: Float)

    private fun weekend(cal: Calendar): Boolean {
        val d = cal.get(Calendar.DAY_OF_WEEK)
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY || d == Calendar.FRIDAY
    }

    fun suggestions(calls: List<CallEntry>, now: Long = System.currentTimeMillis(), limit: Int = 8): List<Pick> {
        if (calls.isEmpty()) return emptyList()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val nowHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        val nowWeekend = weekend(cal)
        val tmp = Calendar.getInstance()
        val scores = HashMap<String, Float>()
        val numbers = HashMap<String, String>()
        for (e in calls) {
            if (e.number.isEmpty()) continue
            val ageDays = (now - e.date) / 86_400_000f
            if (ageDays < 0f || ageDays > 90f) continue
            val weight = when {
                e.isOutgoing -> 1f
                !e.isMissed && e.duration > 0 -> 0.7f
                else -> 0.15f
            }
            tmp.timeInMillis = e.date
            val h = tmp.get(Calendar.HOUR_OF_DAY) + tmp.get(Calendar.MINUTE) / 60f
            val raw = abs(h - nowHour)
            val dh = if (raw > 12f) 24f - raw else raw
            val affinity = exp(-(dh * dh) / 12.5f)
            val dayFactor = if (weekend(tmp) == nowWeekend) 1f else 0.6f
            val recency = exp(-ageDays / 30f)
            val key = PhoneFormat.key(e.number)
            scores[key] = (scores[key] ?: 0f) + weight * recency * (0.35f + 0.65f * affinity) * dayFactor
            numbers.putIfAbsent(key, e.number)
        }
        return scores.entries
            .filter { it.value >= 0.45f }
            .sortedByDescending { it.value }
            .take(limit)
            .map { Pick(numbers.getValue(it.key), it.value) }
    }

    /** Ids of missed calls (last 14 days) whose number was neither called back nor answered since. */
    fun unreturnedMissed(calls: List<CallEntry>, now: Long = System.currentTimeMillis()): Set<Long> {
        val handled = HashSet<String>()
        val out = HashSet<Long>()
        for (e in calls.sortedByDescending { it.date }) {
            val key = PhoneFormat.key(e.number)
            if (key.isEmpty()) continue
            if (e.type == CallLog.Calls.MISSED_TYPE) {
                if (key !in handled && now - e.date < 14L * 86_400_000L) out.add(e.id)
            } else if (e.type != CallLog.Calls.REJECTED_TYPE) {
                handled.add(key)
            }
        }
        return out
    }

    /** Keys of unknown numbers that look like spam: odd digit patterns, or repeatedly ringing without ever being answered. */
    fun spamNumbers(calls: List<CallEntry>, now: Long = System.currentTimeMillis(), isKnown: (String) -> Boolean): Set<String> {
        val missed = HashMap<String, Int>()
        val engaged = HashSet<String>()
        val raw = HashMap<String, String>()
        for (e in calls) {
            val key = PhoneFormat.key(e.number)
            if (key.isEmpty() || now - e.date > 30L * 86_400_000L) continue
            raw.putIfAbsent(key, e.number)
            if (e.isOutgoing || (!e.isMissed && e.duration > 0)) engaged.add(key)
            else if (e.type == CallLog.Calls.MISSED_TYPE) missed[key] = (missed[key] ?: 0) + 1
        }
        val out = HashSet<String>()
        for ((key, number) in raw) {
            if (key in engaged || isKnown(number)) continue
            if (NumberIntel.suspicious(number) || (missed[key] ?: 0) >= 4) out.add(key)
        }
        return out
    }
}
