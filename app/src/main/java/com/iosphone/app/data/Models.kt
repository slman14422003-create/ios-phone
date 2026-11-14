package com.iosphone.app.data

import android.provider.CallLog

data class ContactPhone(val number: String, val label: String)

data class ContactEmail(val address: String, val label: String)

data class Contact(
    val id: Long,
    val lookupKey: String?,
    val name: String,
    val given: String,
    val family: String,
    val company: String,
    val starred: Boolean,
    val thumbUri: String?,
    val photoUri: String?,
    val phones: List<ContactPhone>,
    val emails: List<ContactEmail>,
    val note: String,
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
    val isOutgoing: Boolean
        get() = type == CallLog.Calls.OUTGOING_TYPE
}
