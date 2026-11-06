package com.iosphone.app.telecom

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.telecom.Call
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iosphone.app.data.AppSettings
import com.iosphone.app.data.Recordings
import java.io.File

@Suppress("DEPRECATION")
object CallRecorder {
    var recording by mutableStateOf(false)
        private set

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var owner: Call? = null
    private val suppressed = HashSet<Call>()

    private val sources = intArrayOf(
        MediaRecorder.AudioSource.VOICE_CALL,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.MIC,
    )

    fun onActive(context: Context, call: Call) {
        if (!AppSettings.autoRecord || call in suppressed) return
        start(context, call)
    }

    fun onCallEnded(context: Context, call: Call) {
        suppressed.remove(call)
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

    @Synchronized
    fun start(context: Context, call: Call): Boolean {
        if (owner != null) return owner === call
        val app = context.applicationContext
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        val info = CallManager.info(call)
        val target = Recordings.newFile(app, info.incoming, info.number)
        runCatching { app.startForegroundService(Intent(app, RecordingService::class.java)) }
        var started: MediaRecorder? = null
        for (source in sources) {
            val r = MediaRecorder(app)
            val ok = runCatching {
                r.setAudioSource(source)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioChannels(1)
                r.setAudioSamplingRate(44100)
                r.setAudioEncodingBitRate(96000)
                r.setOutputFile(target.absolutePath)
                r.prepare()
                r.start()
            }.isSuccess
            if (ok) {
                started = r
                break
            }
            runCatching { r.reset() }
            runCatching { r.release() }
            target.delete()
        }
        if (started == null) {
            runCatching { app.stopService(Intent(app, RecordingService::class.java)) }
            return false
        }
        recorder = started
        file = target
        owner = call
        recording = true
        return true
    }

    @Synchronized
    fun stop(context: Context) {
        val r = recorder ?: return
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
