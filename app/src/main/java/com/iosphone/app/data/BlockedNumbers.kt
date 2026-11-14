package com.iosphone.app.data

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BlockedNumbers {
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
