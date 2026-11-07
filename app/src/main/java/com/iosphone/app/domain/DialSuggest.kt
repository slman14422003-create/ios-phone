package com.iosphone.app.domain

import com.iosphone.app.data.Contact
import com.iosphone.app.data.ContactPhone

class Suggestion(val contact: Contact, val phone: ContactPhone, val matchStart: Int, val matchLength: Int)

object DialSuggest {
    private val letters = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9',
    )

    private fun t9(text: String): String {
        val sb = StringBuilder()
        for (ch in text.lowercase()) {
            val d = letters[ch]
            if (d != null) sb.append(d)
        }
        return sb.toString()
    }

    fun find(contacts: List<Contact>, typed: String): Suggestion? {
        val q = typed.filter { it.isDigit() }
        if (q.isEmpty()) return null
        var byContains: Suggestion? = null
        var byName: Suggestion? = null
        for (c in contacts) {
            if (c.phones.isEmpty()) continue
            for (p in c.phones) {
                val d = p.number.filter { it.isDigit() }
                if (d.startsWith(q)) return Suggestion(c, p, 0, q.length)
                if (byContains == null && q.length >= 3) {
                    val at = d.indexOf(q)
                    if (at >= 0) byContains = Suggestion(c, p, at, q.length)
                }
            }
            if (byName == null && q.length >= 2) {
                val words = c.name.split(' ').filter { it.isNotEmpty() }
                val hit = words.any { t9(it).startsWith(q) } || t9(c.name).startsWith(q)
                if (hit) byName = Suggestion(c, c.phones.first(), -1, 0)
            }
        }
        return byContains ?: byName
    }
}
