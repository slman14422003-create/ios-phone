package com.iosphone.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import com.iosphone.app.R
import com.iosphone.app.data.Contact

object QuickCopy {
    fun text(context: Context, value: String) {
        if (value.isEmpty()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText("text", value)
        runCatching { cm.setPrimaryClip(clip) }
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    fun number(context: Context, raw: String) = text(context, PhoneFormat.digits(raw))

    fun card(contact: Contact): String {
        val sb = StringBuilder(contact.name)
        if (contact.company.isNotEmpty()) sb.append('\n').append(contact.company)
        for (p in contact.phones) sb.append('\n').append(PhoneFormat.digits(p.number))
        for (e in contact.emails) sb.append('\n').append(e.address)
        return sb.toString()
    }
}
