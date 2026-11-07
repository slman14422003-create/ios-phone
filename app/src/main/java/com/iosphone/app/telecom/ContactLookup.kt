package com.iosphone.app.telecom

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

class LookupInfo(val name: String?, val photo: String?, val fullPhoto: String? = null)

object ContactLookup {
    fun find(context: Context, number: String): LookupInfo {
        if (number.isEmpty()) return LookupInfo(null, null)
        runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup.PHOTO_URI,
                ),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) return LookupInfo(c.getString(0), c.getString(1), c.getString(2))
            }
        }
        return LookupInfo(null, null)
    }
}
