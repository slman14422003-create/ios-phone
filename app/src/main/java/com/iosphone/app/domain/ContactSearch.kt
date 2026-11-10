package com.iosphone.app.domain

import com.iosphone.app.data.Contact
import java.text.Normalizer

/** Offline smart search: accent/diacritic-insensitive, prefix + typo tolerant, initials, digits in any script. */
object ContactSearch {
    private class Entry(
        val contact: Contact,
        val tokens: List<String>,
        val joined: String,
        val initials: String,
        val company: String,
        val digits: List<String>,
    )

    @Volatile private var cacheSrc: List<Contact>? = null
    @Volatile private var cache: List<Entry> = emptyList()

    fun normalize(s: String): String {
        if (s.isEmpty()) return s
        val d = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        val sb = StringBuilder(d.length)
        for (ch in d) {
            val code = ch.code
            if (code in 0x0300..0x036F) continue
            if (code in 0x064B..0x065F || code == 0x0640 || code == 0x0670) continue
            when (code) {
                0x0623, 0x0625, 0x0622, 0x0671 -> sb.append(0x0627.toChar())
                0x0649, 0x0626 -> sb.append(0x064A.toChar())
                0x0629 -> sb.append(0x0647.toChar())
                0x0624 -> sb.append(0x0648.toChar())
                in 0x0660..0x0669 -> sb.append('0' + (code - 0x0660))
                in 0x06F0..0x06F9 -> sb.append('0' + (code - 0x06F0))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun tokens(s: String): List<String> = normalize(s).split(' ', '-', '.', '_', ',').filter { it.isNotEmpty() }

    private fun entries(all: List<Contact>): List<Entry> {
        if (cacheSrc === all) return cache
        val built = all.map { c ->
            val t = tokens(c.name)
            Entry(
                c,
                t,
                t.joinToString(""),
                t.joinToString("") { it.take(1) },
                normalize(c.company),
                c.phones.map { p -> normalize(p.number).filter { it.isDigit() } },
            )
        }
        cache = built
        cacheSrc = all
        return built
    }

    private fun editWithin(a: String, b: String, max: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > max) return false
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > max) return false
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length] <= max
    }

    private fun tokenScore(q: String, tokens: List<String>): Int {
        var best = 0
        for (t in tokens) {
            val s = when {
                t.startsWith(q) -> 100 - (t.length - q.length).coerceAtMost(30)
                q.length >= 2 && t.contains(q) -> 60
                q.length >= 3 && fuzzy(q, t) -> 40
                else -> 0
            }
            if (s > best) best = s
        }
        return best
    }

    private fun fuzzy(q: String, t: String): Boolean {
        val max = if (q.length <= 5) 1 else 2
        val n = q.length
        for (len in (n - 1)..(n + 1)) {
            if (len < 2 || len > t.length) continue
            if (editWithin(q, t.substring(0, len), max)) return true
        }
        return false
    }

    fun filter(all: List<Contact>, query: String): List<Contact> {
        val raw = query.trim()
        if (raw.isEmpty()) return all
        val q = normalize(raw)
        val qTokens = q.split(' ').filter { it.isNotEmpty() }
        val qDigits = q.filter { it.isDigit() }
        val qJoined = qTokens.joinToString("")
        val scored = ArrayList<Pair<Int, Contact>>()
        for (e in entries(all)) {
            var score = 0
            if (qTokens.isNotEmpty()) {
                var sum = 0
                var all0 = true
                for (qt in qTokens) {
                    val s = tokenScore(qt, e.tokens)
                    if (s == 0) { all0 = false; break }
                    sum += s
                }
                if (all0) score = sum / qTokens.size
                if (score == 0 && qJoined.length in 2..4 && e.initials.startsWith(qJoined)) score = 55
                if (score == 0 && qJoined.length >= 3 && e.joined.contains(qJoined)) score = 50
                if (score == 0 && e.company.isNotEmpty() && qTokens.all { e.company.contains(it) }) score = 30
            }
            if (qDigits.length >= 2 && qDigits.length == qJoined.length) {
                for (d in e.digits) {
                    val s = if (d.startsWith(qDigits)) 80 else if (d.contains(qDigits)) 70 else 0
                    if (s > score) score = s
                }
            }
            if (score > 0) scored.add(score to e.contact)
        }
        return scored.sortedByDescending { it.first }.map { it.second }
    }
}
