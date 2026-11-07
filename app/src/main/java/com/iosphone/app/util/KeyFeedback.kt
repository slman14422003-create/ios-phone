package com.iosphone.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.iosphone.app.data.AppSettings

@Suppress("DEPRECATION")
object KeyFeedback {
    private var handler: Handler? = null
    private var tones: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var toneVolume = -1
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized
    fun start(context: Context) {
        if (handler != null) return
        val thread = HandlerThread("key-feedback", Process.THREAD_PRIORITY_AUDIO)
        thread.start()
        val h = Handler(thread.looper)
        handler = h
        vibrator = runCatching {
            context.applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        }.getOrNull()
        h.post { rebuild(AppSettings.keyVolume) }
    }

    private fun rebuild(volume: Int) {
        runCatching { tones?.release() }
        toneVolume = volume
        tones = runCatching { ToneGenerator(AudioManager.STREAM_DTMF, volume) }.getOrNull()
    }

    private fun toneFor(digit: String): Int = when (digit) {
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> digit[0] - '0'
    }

    fun press(digit: String) {
        val h = handler ?: return
        val sound = AppSettings.keySounds
        val haptic = AppSettings.keyHaptics
        val level = AppSettings.hapticLevel
        val volume = AppSettings.keyVolume
        if (!sound && !haptic) return
        h.post {
            if (sound) {
                if (volume != toneVolume) rebuild(volume)
                runCatching { tones?.startTone(toneFor(digit), 90) }
            }
            if (haptic) buzz(level)
        }
    }

    fun tick() {
        val h = handler ?: return
        if (!AppSettings.keyHaptics) return
        val level = AppSettings.hapticLevel
        h.post { buzz(level) }
    }

    fun pulse(context: Context, millis: Long) {
        val v = vibrator ?: runCatching {
            context.applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        }.getOrNull() ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE), attrs) }
    }

    private fun buzz(level: Int) {
        val v = vibrator ?: return
        val effect = when (level) {
            3 -> VibrationEffect.EFFECT_HEAVY_CLICK
            2 -> VibrationEffect.EFFECT_CLICK
            else -> VibrationEffect.EFFECT_TICK
        }
        runCatching { v.vibrate(VibrationEffect.createPredefined(effect), attrs) }
    }
}
