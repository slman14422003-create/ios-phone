package com.iosphone.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** In-app language: "system", "en" or "ar". Arabic switches the whole UI to right-to-left. */
object LocaleHelper {
    private const val PREFS = "locale_prefs"
    private const val KEY = "lang"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "system") ?: "system"

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Call from attachBaseContext() of every Activity / the Application. */
    fun wrap(base: Context): Context {
        val lang = get(base)
        if (lang == "system") return base
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
