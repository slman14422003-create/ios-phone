package com.iosphone.app.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iosphone.app.data.AppSettings
import com.iosphone.app.data.Recordings
import java.io.File

/**
 * Call recorder. Two things made recordings come out silent before:
 *  1. VOICE_CALL "starts" fine on many phones but delivers digital silence (Android blocks it for normal apps),
 *     and nothing ever checked the result. A watchdog now reads the recorder's amplitude and, if the stream
 *     is silent, restarts on the next source (VOICE_CALL, then VOICE_RECOGNITION, then MIC).
 *  2. The recorder started before the microphone foreground service was actually in the foreground, and
 *     Android then silences the mic. Recording now starts from RecordingService once it is foreground.
 */
@Suppress("DEPRECATION")
object CallRecorder {
    var recording by mutableStateOf(false)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var owner: Call? = null
    private var pending: Call? = null
    private var appContext: Context? = null
    private var sourceIndex = 0
    private var startedAt = 0L
    private var heardSound = false
    private var loggedSound = false
    private val suppressed = HashSet<Call>()

    private val sources = intArrayOf(
        MediaRecorder.AudioSource.VOICE_CALL,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.UNPROCESSED,
        MediaRecorder.AudioSource.CAMCORDER,
    )

    private val sourceNames = arrayOf("VOICE_CALL", "VOICE_COMMUNICATION", "VOICE_RECOGNITION", "MIC", "UNPROCESSED", "CAMCORDER")

    private fun log(message: String) {
        val app = appContext ?: return
        runCatching {
            val f = File(app.filesDir, "recording.log")
            if (f.exists() && f.length() > 64 * 1024) f.delete()
            f.appendText(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()) + " " + message + "\n")
        }
    }

    private const val CHECK_MS = 500L
    private const val SILENCE_LIMIT_MS = 4000L
    private const val SERVICE_WAIT_MS = 900L

    fun onActive(context: Context, call: Call) {
        if (!AppSettings.autoRecord || call in suppressed) return
        start(context, call)
    }

    fun onCallEnded(context: Context, call: Call) {
        suppressed.remove(call)
        if (pending === call) pending = null
        if (owner === call) stop(context)
    }

    fun toggle(context: Context, call: Call) {
        if (owner === call) {
            suppressed.add(call)
            stop(context)
        } else {
            suppressed.remove(call)
            start(context, call)
        }
    }

    /** Asks for the foreground service first; the recorder itself starts in [serviceReady]. */
    fun start(context: Context, call: Call): Boolean {
        if (owner != null) return owner === call
        if (pending != null) return pending === call
        val app = context.applicationContext
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        appContext = app
        pending = call
        val started = runCatching { app.startForegroundService(Intent(app, RecordingService::class.java)) }.isSuccess
        // If the service never reports back, record anyway rather than lose the call.
        handler.postDelayed({ if (pending === call) begin() }, if (started) SERVICE_WAIT_MS else 0L)
        return true
    }

    /** Called by RecordingService right after startForeground succeeded. */
    fun serviceReady() {
        if (pending != null) begin()
    }

    private fun begin() {
        val call = pending ?: return
        val app = appContext ?: return
        pending = null
        if (owner != null) return
        val info = CallManager.info(call)
        val target = Recordings.newFile(app, info.incoming, info.number)
        sourceIndex = 0
        if (!open(app, target, 0)) {
            runCatching { app.stopService(Intent(app, RecordingService::class.java)) }
            return
        }
        owner = call
        file = target
        recording = true
        heardSound = false
        loggedSound = false
        startedAt = System.currentTimeMillis()
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, CHECK_MS)
    }

    /** Tries sources from [from] on until one starts; returns false if none does. */
    private fun open(app: Context, target: File, from: Int): Boolean {
        for (i in from until sources.size) {
            val r = MediaRecorder(app)
            val ok = runCatching {
                r.setAudioSource(sources[i])
                runCatching { r.setPrivacySensitive(false) }
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioChannels(1)
                r.setAudioSamplingRate(44100)
                r.setAudioEncodingBitRate(96000)
                r.setOutputFile(target.absolutePath)
                r.prepare()
                r.start()
            }.isSuccess
            log("open " + sourceNames[i] + " ok=" + ok)
            if (ok) {
                recorder = r
                sourceIndex = i
                return true
            }
            runCatching { r.reset() }
            runCatching { r.release() }
            target.delete()
        }
        return false
    }

    private val watchdog = object : Runnable {
        override fun run() {
            val r = recorder ?: return
            val amp = runCatching { r.maxAmplitude }.getOrDefault(0)
            if (amp > 0) heardSound = true
            if (heardSound && !loggedSound) {
                loggedSound = true
                log("sound heard on " + sourceNames[sourceIndex] + " amp=" + amp)
            }
            val elapsed = System.currentTimeMillis() - startedAt
            if (!heardSound && elapsed >= SILENCE_LIMIT_MS && sourceIndex < sources.lastIndex) {
                switchSource()
            }
            // Once sound has been heard, or the last source is in use, there is nothing left to correct.
            if (!heardSound && sourceIndex < sources.lastIndex) handler.postDelayed(this, CHECK_MS)
        }
    }

    private fun switchSource() {
        val app = appContext ?: return
        val target = file ?: return
        log("silent on " + sourceNames[sourceIndex] + ", switching")
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        target.delete()
        if (open(app, target, sourceIndex + 1)) {
            startedAt = System.currentTimeMillis()
            heardSound = false
            loggedSound = false
        } else {
            owner = null
            recording = false
            file = null
            runCatching { app.stopService(Intent(app, RecordingService::class.java)) }
        }
    }

    fun stop(context: Context) {
        handler.removeCallbacks(watchdog)
        pending = null
        val r = recorder
        if (r == null) {
            val app = context.applicationContext
            runCatching { app.stopService(Intent(app, RecordingService::class.java)) }
            return
        }
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        owner = null
        recording = false
        file?.let { if (it.length() < 2048) it.delete() }
        file = null
        val app = context.applicationContext
        runCatching { app.stopService(Intent(app, RecordingService::class.java)) }
    }
}
