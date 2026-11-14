package com.iosphone.app.data

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CallLogRepository {
    suspend fun load(context: Context): List<CallEntry> = withContext(Dispatchers.IO) {
        val args = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, 500)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(CallLog.Calls.DATE))
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }
        val out = ArrayList<CallEntry>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.IS_READ,
            ),
            args,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(
                    CallEntry(
                        id = c.getLong(0),
                        number = c.getString(1).orEmpty(),
                        name = c.getString(2)?.takeIf { it.isNotBlank() },
                        type = c.getInt(3),
                        date = c.getLong(4),
                        duration = c.getLong(5),
                        isRead = c.isNull(6) || c.getInt(6) != 0,
                    ),
                )
            }
        }
        out
    }

    suspend fun delete(context: Context, ids: List<Long>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext true
        runCatching {
            val where = CallLog.Calls._ID + " IN (" + ids.joinToString(",") + ")"
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, where, null) > 0
        }.getOrDefault(false)
    }
}
