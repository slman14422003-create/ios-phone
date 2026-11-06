package com.iosphone.app.telecom

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CallInfo(
    val call: Call,
    val state: Int,
    val number: String,
    val displayName: String?,
    val connectTime: Long,
    val incoming: Boolean,
)

@Suppress("DEPRECATION")
object CallManager {
    val calls = mutableStateListOf<Call>()
    var tick by mutableIntStateOf(0)
    var muted by mutableStateOf(false)
    var route by mutableIntStateOf(CallAudioState.ROUTE_EARPIECE)
    var service: CallService? = null
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state != Call.STATE_RINGING) service?.cancelIncoming()
            tick++
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            tick++
        }
    }

    fun add(call: Call) {
        call.registerCallback(callback)
        calls.add(call)
        tick++
    }

    fun remove(call: Call) {
        call.unregisterCallback(callback)
        calls.remove(call)
        tick++
    }

    fun primary(): Call? {
        tick
        return calls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: calls.firstOrNull { it.state != Call.STATE_HOLDING }
            ?: calls.firstOrNull()
    }

    fun info(call: Call): CallInfo {
        val d = call.details
        val number = d?.handle?.schemeSpecificPart.orEmpty()
        return CallInfo(
            call = call,
            state = call.state,
            number = number,
            displayName = d?.callerDisplayName?.takeIf { it.isNotBlank() },
            connectTime = d?.connectTimeMillis ?: 0L,
            incoming = d?.callDirection == Call.Details.DIRECTION_INCOMING,
        )
    }

    fun answer(call: Call) {
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject(call: Call) {
        call.reject(false, null)
    }

    fun hangup(call: Call) {
        call.disconnect()
    }

    fun hold(call: Call) {
        call.hold()
    }

    fun unhold(call: Call) {
        call.unhold()
    }

    fun swapTo(target: Call) {
        calls.filter { it !== target && it.state == Call.STATE_ACTIVE }.forEach { it.hold() }
        target.unhold()
    }

    fun merge(call: Call, other: Call) {
        runCatching { call.conference(other) }
    }

    fun applyMute(value: Boolean) {
        service?.setMuted(value)
        muted = value
    }

    fun setSpeaker(on: Boolean) {
        service?.setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }

    fun dtmf(call: Call, digit: Char) {
        call.playDtmfTone(digit)
        handler.postDelayed({ call.stopDtmfTone() }, 160)
    }
}
