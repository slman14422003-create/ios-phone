package com.iosphone.app.data

import android.provider.CallLog

data class ContactPhone(val number: String, val label: String)

data class Contact(
    val id: Long,
    val name: String,
    val starred: Boolean,
    val photoUri: String?,
    val phones: List<ContactPhone>,
)

data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
) {
    val isMissed: Boolean
        get() = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE
}
