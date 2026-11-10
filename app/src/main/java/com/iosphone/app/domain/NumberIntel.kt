package com.iosphone.app.domain

import android.content.ClipboardManager
import android.content.Context
import com.iosphone.app.util.PhoneFormat

object NumberIntel {
    private val clipShape = Regex("""^\+?[\d\s().\-]{6,24}$""")
    private val inText = Regex("""(?<![\d])\+?\d[\d\s().\-]{5,18}\d(?![\d])""")

    fun clipboardNumber(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val desc = cm.primaryClipDescription ?: return null
        if (!desc.hasMimeType("text/*")) return null
        val raw = runCatching { cm.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull()?.trim().orEmpty()
        if (raw.length < 6 || raw.length > 26 || !clipShape.matches(raw)) return null
        val digits = PhoneFormat.digits(raw)
        val count = digits.count { it.isDigit() }
        return if (count in 7..15) digits else null
    }

    fun extract(text: String): List<String> {
        if (text.length < 7) return emptyList()
        val out = LinkedHashSet<String>()
        for (m in inText.findAll(text)) {
            val d = PhoneFormat.digits(m.value)
            if (d.count { it.isDigit() } in 7..15) out.add(d)
        }
        return out.toList()
    }

    fun suspicious(number: String): Boolean {
        val d = number.filter { it.isDigit() }
        if (d.length < 7) return false
        if (d.toSet().size == 1) return true
        var run = 1
        for (i in 1 until d.length) {
            run = if (d[i] - d[i - 1] == 1) run + 1 else 1
            if (run >= 7) return true
        }
        return d.takeLast(6).all { it == '0' }
    }
}
