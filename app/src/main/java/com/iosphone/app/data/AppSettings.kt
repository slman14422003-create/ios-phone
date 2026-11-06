package com.iosphone.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Value of [CallBgSettings.preset] when the user picked their own photo. */
const val BG_CUSTOM = -1

/** Background settings for one kind of call screen (incoming, or outgoing/active). */
class CallBgSettings(private val key: String) {
    var preset by mutableIntStateOf(0)
        private set
    var blur by mutableFloatStateOf(0.55f)
        private set
    var dim by mutableFloatStateOf(0.18f)
        private set

    /** Bumped every time a new photo is imported so the screen reloads it. */
    var customVersion by mutableIntStateOf(0)
        private set

    private var prefs: SharedPreferences? = null

    internal fun attach(p: SharedPreferences) {
        prefs = p
        preset = p.getInt("${key}_preset", 0)
        blur = p.getFloat("${key}_blur", 0.55f)
        dim = p.getFloat("${key}_dim", 0.18f)
        customVersion = p.getInt("${key}_ver", 0)
    }

    fun file(context: Context): File = File(context.filesDir, "call_bg_$key.jpg")

    fun hasCustom(context: Context): Boolean = file(context).exists()

    fun selectPreset(value: Int) {
        preset = value
        prefs?.edit()?.putInt("${key}_preset", value)?.apply()
    }

    fun changeBlur(value: Float) {
        blur = value.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat("${key}_blur", blur)?.apply()
    }

    fun changeDim(value: Float) {
        dim = value.coerceIn(0f, 0.8f)
        prefs?.edit()?.putFloat("${key}_dim", dim)?.apply()
    }

    fun customImported() {
        customVersion += 1
        preset = BG_CUSTOM
        prefs?.edit()?.putInt("${key}_ver", customVersion)?.putInt("${key}_preset", BG_CUSTOM)?.apply()
    }
}

object AppSettings {
    private var prefs: SharedPreferences? = null

    /** 0 = follow system, 1 = light, 2 = dark. */
    var appearance by mutableIntStateOf(0)
        private set
    var keySounds by mutableStateOf(true)
        private set
    var keyHaptics by mutableStateOf(true)
        private set

    val incoming = CallBgSettings("in")
    val active = CallBgSettings("active")

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("ios_phone_settings", Context.MODE_PRIVATE)
        prefs = p
        appearance = p.getInt("appearance", 0)
        keySounds = p.getBoolean("key_sounds", true)
        keyHaptics = p.getBoolean("key_haptics", true)
        incoming.attach(p)
        active.attach(p)
    }

    fun setAppearance(value: Int) {
        appearance = value
        prefs?.edit()?.putInt("appearance", value)?.apply()
    }

    fun setKeySounds(value: Boolean) {
        keySounds = value
        prefs?.edit()?.putBoolean("key_sounds", value)?.apply()
    }

    fun setKeyHaptics(value: Boolean) {
        keyHaptics = value
        prefs?.edit()?.putBoolean("key_haptics", value)?.apply()
    }
}
