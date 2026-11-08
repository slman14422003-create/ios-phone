package com.iosphone.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

const val BG_PHOTO = -1
const val BG_COLORS = -2

@Immutable
data class CallLook(
    val bg: Int = 0,
    val c1: Int = 0xFF3A4F7A.toInt(),
    val c2: Int = 0xFF5B4B8A.toInt(),
    val c3: Int = 0xFF141A25.toInt(),
    val angle: Float = 180f,
    val blur: Float = 0.55f,
    val dim: Float = 0.18f,
    val glow: Float = 0.8f,
    val vignette: Float = 0.3f,
    val grain: Float = 0.1f,
    val animated: Boolean = false,
    val contactPhoto: Boolean = false,
    val textMode: Int = 0,
    val nameSize: Float = 0.45f,
    val showNumber: Boolean = false,
    val showSim: Boolean = true,
    val accent: Int = 0xFFFFFFFF.toInt(),
    val accept: Int = 0xFF34C759.toInt(),
    val decline: Int = 0xFFFF3B30.toInt(),
    val buttonStyle: Int = 0,
    val buttonShape: Int = 0,
    val avatarShape: Int = 0,
    val avatarSize: Float = 0.5f,
    val ring: Int = 0,
    val avatarActive: Boolean = false,
    val photoVersion: Int = 0,
)

private fun readLook(p: SharedPreferences, k: String): CallLook {
    val d = CallLook()
    fun f(name: String, def: Float, lo: Float, hi: Float) = p.getFloat("${k}_$name", def).coerceIn(lo, hi)
    fun i(name: String, def: Int, lo: Int, hi: Int) = p.getInt("${k}_$name", def).coerceIn(lo, hi)
    return CallLook(
        bg = p.getInt("${k}_preset", d.bg),
        c1 = p.getInt("${k}_c1", d.c1),
        c2 = p.getInt("${k}_c2", d.c2),
        c3 = p.getInt("${k}_c3", d.c3),
        angle = f("angle", d.angle, 0f, 360f),
        blur = f("blur", d.blur, 0f, 1f),
        dim = f("dim", d.dim, 0f, 0.8f),
        glow = f("glow", d.glow, 0f, 1f),
        vignette = f("vignette", d.vignette, 0f, 1f),
        grain = f("grain", d.grain, 0f, 1f),
        animated = p.getBoolean("${k}_animated", d.animated),
        contactPhoto = p.getBoolean("${k}_contact_photo", d.contactPhoto),
        textMode = i("text_mode", d.textMode, 0, 2),
        nameSize = f("name_size", d.nameSize, 0f, 1f),
        showNumber = p.getBoolean("${k}_show_number", d.showNumber),
        showSim = p.getBoolean("${k}_show_sim", d.showSim),
        accent = p.getInt("${k}_accent", d.accent),
        accept = p.getInt("${k}_accept", d.accept),
        decline = p.getInt("${k}_decline", d.decline),
        buttonStyle = i("button_style", d.buttonStyle, 0, 2),
        buttonShape = i("button_shape", d.buttonShape, 0, 1),
        avatarShape = i("avatar_shape", d.avatarShape, 0, 2),
        avatarSize = f("avatar_size", d.avatarSize, 0f, 1f),
        ring = i("ring", d.ring, 0, 2),
        avatarActive = p.getBoolean("${k}_avatar_active", d.avatarActive),
        photoVersion = p.getInt("${k}_ver", d.photoVersion),
    )
}

private fun writeLook(e: SharedPreferences.Editor, k: String, l: CallLook) {
    e.putInt("${k}_preset", l.bg)
    e.putInt("${k}_c1", l.c1)
    e.putInt("${k}_c2", l.c2)
    e.putInt("${k}_c3", l.c3)
    e.putFloat("${k}_angle", l.angle)
    e.putFloat("${k}_blur", l.blur)
    e.putFloat("${k}_dim", l.dim)
    e.putFloat("${k}_glow", l.glow)
    e.putFloat("${k}_vignette", l.vignette)
    e.putFloat("${k}_grain", l.grain)
    e.putBoolean("${k}_animated", l.animated)
    e.putBoolean("${k}_contact_photo", l.contactPhoto)
    e.putInt("${k}_text_mode", l.textMode)
    e.putFloat("${k}_name_size", l.nameSize)
    e.putBoolean("${k}_show_number", l.showNumber)
    e.putBoolean("${k}_show_sim", l.showSim)
    e.putInt("${k}_accent", l.accent)
    e.putInt("${k}_accept", l.accept)
    e.putInt("${k}_decline", l.decline)
    e.putInt("${k}_button_style", l.buttonStyle)
    e.putInt("${k}_button_shape", l.buttonShape)
    e.putInt("${k}_avatar_shape", l.avatarShape)
    e.putFloat("${k}_avatar_size", l.avatarSize)
    e.putInt("${k}_ring", l.ring)
    e.putBoolean("${k}_avatar_active", l.avatarActive)
    e.putInt("${k}_ver", l.photoVersion)
}

class CallBgSettings(private val key: String) {
    var look by mutableStateOf(CallLook())
        private set

    private var prefs: SharedPreferences? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saver = Runnable { persist() }

    internal fun attach(p: SharedPreferences) {
        prefs = p
        look = readLook(p, key)
    }

    fun file(context: Context): File = File(context.filesDir, "call_bg_$key.jpg")

    fun hasCustom(context: Context): Boolean = file(context).exists()

    fun update(block: (CallLook) -> CallLook) {
        look = block(look)
        handler.removeCallbacks(saver)
        handler.postDelayed(saver, 250)
    }

    fun flush() {
        handler.removeCallbacks(saver)
        persist()
    }

    fun photoImported() {
        update { it.copy(bg = BG_PHOTO, photoVersion = it.photoVersion + 1) }
        flush()
    }

    private fun persist() {
        val p = prefs ?: return
        val e = p.edit()
        writeLook(e, key, look)
        e.apply()
    }
}

class SpeedEntry(val name: String, val number: String)

object AppSettings {
    private var prefs: SharedPreferences? = null

    var appearance by mutableIntStateOf(0)
        private set
    var keySounds by mutableStateOf(true)
        private set
    var keyHaptics by mutableStateOf(true)
        private set
    var hapticLevel by mutableIntStateOf(1)
        private set
    var keyVolume by mutableIntStateOf(70)
        private set
    var autoRecord by mutableStateOf(true)
        private set
    var recordSpeaker by mutableStateOf(true)
        private set
    var defaultSim by mutableStateOf("")
        private set
    var roamingAssist by mutableStateOf(true)
        private set
    var autoAnswerSeconds by mutableIntStateOf(0)
        private set
    var vibrateOnAnswer by mutableStateOf(false)
        private set
    var vibrateOnEnd by mutableStateOf(false)
        private set
    var flashOnRing by mutableStateOf(false)
        private set
    var incomingDisplay by mutableIntStateOf(0)
        private set
    var quickReplies by mutableStateOf<List<String>?>(null)
        private set
    var speedDial by mutableStateOf<Map<Int, SpeedEntry>>(emptyMap())
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
        hapticLevel = p.getInt("haptic_level", 1).coerceIn(1, 3)
        keyVolume = p.getInt("key_volume", 70).coerceIn(0, 100)
        autoRecord = p.getBoolean("auto_record", true)
        recordSpeaker = p.getBoolean("record_speaker", true)
        defaultSim = p.getString("default_sim", "").orEmpty()
        roamingAssist = p.getBoolean("roaming_assist", true)
        autoAnswerSeconds = p.getInt("auto_answer", 0).coerceIn(0, 30)
        vibrateOnAnswer = p.getBoolean("vibrate_answer", false)
        vibrateOnEnd = p.getBoolean("vibrate_end", false)
        flashOnRing = p.getBoolean("flash_ring", false)
        incomingDisplay = p.getInt("incoming_display", 0).coerceIn(0, 1)
        quickReplies = p.getString("quick_replies", null)?.split('\u001F')?.filter { it.isNotBlank() }
        val speed = HashMap<Int, SpeedEntry>()
        for (n in 2..9) {
            val number = p.getString("speed_${n}_number", null)
            if (!number.isNullOrBlank()) speed[n] = SpeedEntry(p.getString("speed_${n}_name", "").orEmpty(), number)
        }
        speedDial = speed
        incoming.attach(p)
        active.attach(p)
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        val e = prefs?.edit() ?: return
        e.block()
        e.apply()
    }

    @JvmName("applyAppearance")
    fun setAppearance(value: Int) {
        appearance = value
        edit { putInt("appearance", value) }
    }

    @JvmName("applyKeySounds")
    fun setKeySounds(value: Boolean) {
        keySounds = value
        edit { putBoolean("key_sounds", value) }
    }

    @JvmName("applyKeyHaptics")
    fun setKeyHaptics(value: Boolean) {
        keyHaptics = value
        edit { putBoolean("key_haptics", value) }
    }

    fun changeHapticLevel(value: Int) {
        hapticLevel = value.coerceIn(1, 3)
        edit { putInt("haptic_level", hapticLevel) }
    }

    fun changeKeyVolume(value: Int) {
        keyVolume = value.coerceIn(0, 100)
        edit { putInt("key_volume", keyVolume) }
    }

    fun changeRecordSpeaker(value: Boolean) {
        recordSpeaker = value
        edit { putBoolean("record_speaker", value) }
    }

    fun changeAutoRecord(value: Boolean) {
        autoRecord = value
        edit { putBoolean("auto_record", value) }
    }

    fun changeDefaultSim(value: String) {
        defaultSim = value
        edit { putString("default_sim", value) }
    }

    fun changeRoamingAssist(value: Boolean) {
        roamingAssist = value
        edit { putBoolean("roaming_assist", value) }
    }


    fun changeAutoAnswer(value: Int) {
        autoAnswerSeconds = value.coerceIn(0, 30)
        edit { putInt("auto_answer", autoAnswerSeconds) }
    }

    fun changeVibrateOnAnswer(value: Boolean) {
        vibrateOnAnswer = value
        edit { putBoolean("vibrate_answer", value) }
    }

    fun changeVibrateOnEnd(value: Boolean) {
        vibrateOnEnd = value
        edit { putBoolean("vibrate_end", value) }
    }

    fun changeFlashOnRing(value: Boolean) {
        flashOnRing = value
        edit { putBoolean("flash_ring", value) }
    }

    fun changeIncomingDisplay(value: Int) {
        incomingDisplay = value.coerceIn(0, 1)
        edit { putInt("incoming_display", incomingDisplay) }
    }

    fun changeQuickReplies(value: List<String>?) {
        val clean = value?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(8)
        quickReplies = clean
        edit {
            if (clean == null) remove("quick_replies") else putString("quick_replies", clean.joinToString("\u001F"))
        }
    }

    fun changeSpeedDial(slot: Int, entry: SpeedEntry?) {
        if (slot !in 2..9) return
        val next = HashMap(speedDial)
        if (entry == null) next.remove(slot) else next[slot] = entry
        speedDial = next
        edit {
            if (entry == null) {
                remove("speed_${slot}_number")
                remove("speed_${slot}_name")
            } else {
                putString("speed_${slot}_number", entry.number)
                putString("speed_${slot}_name", entry.name)
            }
        }
    }
}
