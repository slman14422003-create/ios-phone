package com.iosphone.app.data

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts as ContactsTable
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactsRepository {
    suspend fun load(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val phones = HashMap<Long, MutableList<ContactPhone>>()
        resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val number = c.getString(1) ?: continue
                val label = Phone.getTypeLabel(context.resources, c.getInt(2), c.getString(3))
                    .toString()
                    .lowercase()
                val list = phones.getOrPut(id) { mutableListOf() }
                val digits = number.filter { it.isDigit() }
                if (list.none { it.number.filter { ch -> ch.isDigit() } == digits }) {
                    list.add(ContactPhone(number, label))
                }
            }
        }
        val out = ArrayList<Contact>()
        resolver.query(
            ContactsTable.CONTENT_URI,
            arrayOf(
                ContactsTable._ID,
                ContactsTable.DISPLAY_NAME_PRIMARY,
                ContactsTable.STARRED,
                ContactsTable.PHOTO_THUMBNAIL_URI,
            ),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val id = c.getLong(0)
                out.add(
                    Contact(
                        id = id,
                        name = name,
                        starred = c.getInt(2) == 1,
                        photoUri = c.getString(3),
                        phones = phones[id].orEmpty(),
                    ),
                )
            }
        }
        val collator = Collator.getInstance()
        out.sortWith(Comparator { a, b -> collator.compare(a.name, b.name) })
        out
    }
}
