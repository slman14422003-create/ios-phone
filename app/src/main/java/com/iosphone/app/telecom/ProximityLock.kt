package com.iosphone.app.telecom

import android.content.Context
import android.os.PowerManager

object ProximityLock {
    private var lock: PowerManager.WakeLock? = null

    @Synchronized
    fun update(context: Context, enabled: Boolean) {
        if (!enabled) {
            release()
            return
        }
        if (lock != null) return
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
        lock = runCatching {
            pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "iosphone:proximity").also {
                it.acquire(4 * 60 * 60 * 1000L)
            }
        }.getOrNull()
    }

    @Synchronized
    fun release() {
        runCatching { lock?.let { if (it.isHeld) it.release() } }
        lock = null
    }
}
