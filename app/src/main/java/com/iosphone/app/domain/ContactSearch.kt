package com.iosphone.app.domain

import com.iosphone.app.data.Contact

object ContactSearch {
    fun filter(all: List<Contact>, query: String): List<Contact> {
        val q = query.trim()
        if (q.isEmpty()) return all
        val digits = q.filter { it.isDigit() }
        val exact = ArrayList<Contact>()
        val partial = ArrayList<Contact>()
        for (c in all) {
            val nameHit = c.name.contains(q, ignoreCase = true)
            val numberHit = digits.isNotEmpty() &&
                c.phones.any { p -> p.number.filter { it.isDigit() }.contains(digits) }
            if (nameHit && c.name.startsWith(q, ignoreCase = true)) exact.add(c)
            else if (nameHit || numberHit) partial.add(c)
        }
        return exact + partial
    }
}
