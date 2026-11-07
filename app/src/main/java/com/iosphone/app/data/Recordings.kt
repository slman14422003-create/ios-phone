package com.iosphone.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.iosphone.app.util.PhoneFormat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Recording(val file: File, val time: Long, val incoming: Boolean, val number: String) {
    val key: String get() = PhoneFormat.key(number)
}

object Recordings {
    fun dir(context: Context): File = File(context.filesDir, "recordings").also { it.mkdirs() }

    fun newFile(context: Context, incoming: Boolean, number: String): File {
        val clean = PhoneFormat.digits(number).ifEmpty { "unknown" }
        return File(dir(context), "${System.currentTimeMillis()}_${if (incoming) "in" else "out"}_$clean.m4a")
    }

    suspend fun load(context: Context): List<Recording> = withContext(Dispatchers.IO) {
        dir(context).listFiles()
            ?.mapNotNull { parse(it) }
            ?.filter { it.file.length() > 0 }
            ?.sortedByDescending { it.time }
            .orEmpty()
    }

    private fun parse(f: File): Recording? {
        if (f.extension != "m4a") return null
        val parts = f.nameWithoutExtension.split('_', limit = 3)
        if (parts.size < 3) return null
        val time = parts[0].toLongOrNull() ?: return null
        return Recording(f, time, parts[1] == "in", parts[2])
    }

    suspend fun durationMs(file: File): Long = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { r.release() }
        }
    }
}
