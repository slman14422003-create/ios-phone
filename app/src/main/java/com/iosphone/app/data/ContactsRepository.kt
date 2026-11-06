package com.iosphone.app.data

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
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
        val emails = HashMap<Long, MutableList<ContactEmail>>()
        resolver.query(
            Email.CONTENT_URI,
            arrayOf(Email.CONTACT_ID, Email.ADDRESS, Email.TYPE, Email.LABEL),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val address = c.getString(1) ?: continue
                val label = Email.getTypeLabel(context.resources, c.getInt(2), c.getString(3))
                    .toString()
                    .lowercase()
                val list = emails.getOrPut(id) { mutableListOf() }
                if (list.none { it.address.equals(address, ignoreCase = true) }) {
                    list.add(ContactEmail(address, label))
                }
            }
        }
        val given = HashMap<Long, String>()
        val family = HashMap<Long, String>()
        val company = HashMap<Long, String>()
        val notes = HashMap<Long, String>()
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
            ),
            ContactsContract.Data.MIMETYPE + " IN (?,?,?)",
            arrayOf(StructuredName.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE, Note.CONTENT_ITEM_TYPE),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                when (c.getString(1)) {
                    StructuredName.CONTENT_ITEM_TYPE -> {
                        if (!given.containsKey(id)) {
                            given[id] = c.getString(3).orEmpty()
                            family[id] = c.getString(4).orEmpty()
                        }
                    }
                    Organization.CONTENT_ITEM_TYPE -> {
                        if (!company.containsKey(id)) company[id] = c.getString(2).orEmpty()
                    }
                    Note.CONTENT_ITEM_TYPE -> {
                        if (!notes.containsKey(id)) notes[id] = c.getString(2).orEmpty()
                    }
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
                ContactsTable.PHOTO_URI,
                ContactsTable.LOOKUP_KEY,
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
                        lookupKey = c.getString(5),
                        name = name,
                        given = given[id].orEmpty(),
                        family = family[id].orEmpty(),
                        company = company[id].orEmpty(),
                        starred = c.getInt(2) == 1,
                        thumbUri = c.getString(3),
                        photoUri = c.getString(4),
                        phones = phones[id].orEmpty(),
                        emails = emails[id].orEmpty(),
                        note = notes[id].orEmpty(),
                    ),
                )
            }
        }
        val collator = Collator.getInstance()
        out.sortWith(Comparator { a, b -> collator.compare(a.name, b.name) })
        out
    }
}
