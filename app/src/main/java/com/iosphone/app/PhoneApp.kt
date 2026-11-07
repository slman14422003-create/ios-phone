package com.iosphone.app

import android.app.Application
import android.content.Context
import com.iosphone.app.data.AppSettings
import com.iosphone.app.util.LocaleHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val log = File(filesDir, "crash.log")
                if (log.exists() && log.length() > 256 * 1024) log.delete()
                log.appendText("$stamp ${thread.name}\n${error.stackTraceToString()}\n\n")
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
