package com.iosphone.app.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactsWriter {
    suspend fun save(
        context: Context,
        existing: Contact?,
        given: String,
        family: String,
        company: String,
        phones: List<String>,
        emails: List<String>,
        note: String,
        phoneLabels: List<String> = emptyList(),
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val ops = ArrayList<ContentProviderOperation>()
            var rawId: Long? = null
            if (existing != null) {
                resolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    ContactsContract.RawContacts.CONTACT_ID + "=?",
                    arrayOf(existing.id.toString()),
                    null,
                )?.use { c -> if (c.moveToFirst()) rawId = c.getLong(0) }
                val id = rawId ?: return@runCatching false
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + " IN (?,?,?,?,?)",
                            arrayOf(
                                id.toString(),
                                StructuredName.CONTENT_ITEM_TYPE,
                                Phone.CONTENT_ITEM_TYPE,
                                Email.CONTENT_ITEM_TYPE,
                                Note.CONTENT_ITEM_TYPE,
                                Organization.CONTENT_ITEM_TYPE,
                            ),
                        )
                        .build(),
                )
            } else {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build(),
                )
            }
            fun dataInsert(mime: String): ContentProviderOperation.Builder {
                val b = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                if (existing != null) {
                    b.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                } else {
                    b.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                }
                return b.withValue(ContactsContract.Data.MIMETYPE, mime)
            }
            if (given.isNotBlank() || family.isNotBlank()) {
                ops.add(
                    dataInsert(StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.GIVEN_NAME, given.trim())
                        .withValue(StructuredName.FAMILY_NAME, family.trim())
                        .build(),
                )
            }
            phones.forEachIndexed { index, raw ->
                val number = raw.trim()
                if (number.isNotEmpty()) {
                    ops.add(
                        dataInsert(Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, number)
                            .withValue(Phone.TYPE, PhoneLabels.typeOf(phoneLabels.getOrNull(index)))
                            .build(),
                    )
                }
            }
            for (e in emails.map { it.trim() }.filter { it.isNotEmpty() }) {
                ops.add(
                    dataInsert(Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, e)
                        .withValue(Email.TYPE, Email.TYPE_HOME)
                        .build(),
                )
            }
            if (company.isNotBlank()) {
                ops.add(
                    dataInsert(Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, company.trim())
                        .build(),
                )
            }
            if (note.isNotBlank()) {
                ops.add(
                    dataInsert(Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, note.trim())
                        .build(),
                )
            }
            resolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }

    suspend fun delete(context: Context, id: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.delete(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                null,
                null,
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun setStarred(context: Context, id: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
                values,
                null,
                null,
            ) > 0
        }.getOrDefault(false)
    }

    suspend fun block(context: Context, number: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!BlockedNumberContract.canCurrentUserBlockNumbers(context)) return@runCatching false
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values) != null
        }.getOrDefault(false)
    }
}

object PhoneLabels {
    val all = listOf("mobile", "home", "work", "main", "other")

    fun normalize(label: String?): String {
        val l = label?.trim()?.lowercase().orEmpty()
        return all.firstOrNull { l == it } ?: if (l.isEmpty()) "mobile" else "other"
    }

    fun typeOf(label: String?): Int = when (normalize(label)) {
        "home" -> Phone.TYPE_HOME
        "work" -> Phone.TYPE_WORK
        "main" -> Phone.TYPE_MAIN
        "other" -> Phone.TYPE_OTHER
        else -> Phone.TYPE_MOBILE
    }
}
