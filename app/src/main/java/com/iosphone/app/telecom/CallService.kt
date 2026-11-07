package com.iosphone.app.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.iosphone.app.InCallActivity
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.util.KeyFeedback
import com.iosphone.app.util.PhoneFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class CallService : InCallService() {
    private val main = Handler(Looper.getMainLooper())
    private var io: ExecutorService? = null
    private var flashing = false
    private var torchOn = false
    private var torchId: String? = null
    private var autoAnswer: Runnable? = null
    private val blink = object : Runnable {
        override fun run() {
            torchOn = !torchOn
            setTorch(torchOn)
            main.postDelayed(this, 450)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        CallManager.service = this
        io = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        stopRingExtras()
        io?.shutdownNow()
        io = null
        CallRecorder.stop(this)
        ProximityLock.release()
        if (CallManager.service === this) CallManager.service = null
        super.onDestroy()
    }

    fun refreshProximity() {
        val inCall = CallManager.calls.any { it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING }
        val nearEar = CallManager.route == CallAudioState.ROUTE_EARPIECE || CallManager.route == CallAudioState.ROUTE_WIRED_HEADSET
        ProximityLock.update(this, inCall && nearEar)
    }

    override fun onCallAdded(call: Call) {
        CallManager.add(call)
        if (call.state == Call.STATE_RINGING) {
            showIncoming(call)
            startRingExtras(call)
        } else {
            openUi()
        }
    }

    override fun onCallRemoved(call: Call) {
        CallManager.remove(call)
        if (CallManager.calls.none { it.state == Call.STATE_RINGING }) stopRingExtras()
        cancelIncoming()
        refreshProximity()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        if (audioState != null) {
            CallManager.muted = audioState.isMuted
            CallManager.route = audioState.route
        }
        refreshProximity()
    }

    fun onStateEvent(state: Int) {
        if (state == Call.STATE_ACTIVE && AppSettings.vibrateOnAnswer) KeyFeedback.pulse(this, 70)
        if (state == Call.STATE_DISCONNECTED && AppSettings.vibrateOnEnd) KeyFeedback.pulse(this, 110)
        if (state != Call.STATE_RINGING) stopRingExtras()
    }

    private fun setTorch(on: Boolean) {
        runCatching {
            val cm = getSystemService(CameraManager::class.java)
            val id = torchId ?: cm.cameraIdList.firstOrNull {
                val c = cm.getCameraCharacteristics(it)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }.also { torchId = it }
            if (id != null) cm.setTorchMode(id, on)
        }
    }

    private fun headsetConnected(): Boolean {
        val am = getSystemService(AudioManager::class.java)
        val types = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
        )
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in types }
    }

    private fun startRingExtras(call: Call) {
        stopRingExtras()
        if (AppSettings.flashOnRing) {
            flashing = true
            main.post(blink)
        }
        val seconds = AppSettings.autoAnswerSeconds
        if (seconds > 0 && headsetConnected()) {
            val r = Runnable { if (call.state == Call.STATE_RINGING) CallManager.answer(call) }
            autoAnswer = r
            main.postDelayed(r, seconds * 1000L)
        }
    }

    private fun stopRingExtras() {
        main.removeCallbacks(blink)
        autoAnswer?.let { main.removeCallbacks(it) }
        autoAnswer = null
        if (flashing) {
            flashing = false
            torchOn = false
            setTorch(false)
        }
    }

    fun cancelIncoming() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun openUi() {
        val intent = Intent(this, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { startActivity(intent) }
    }

    private fun showIncoming(call: Call) {
        val number = CallManager.info(call).number
        if (AppSettings.incomingDisplay == 1) openUi()
        val worker = io
        if (worker == null) {
            postIncoming(call, PhoneFormat.format(number).ifEmpty { getString(R.string.unknown) })
            return
        }
        worker.execute {
            val found = runCatching { ContactLookup.find(this, number).name }.getOrNull()
            main.post {
                if (call.state == Call.STATE_RINGING) {
                    postIncoming(call, found ?: PhoneFormat.format(number).ifEmpty { getString(R.string.unknown) })
                }
            }
        }
    }

    private fun postIncoming(call: Call, name: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.incoming_channel), NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null)
        channel.enableVibration(false)
        nm.createNotificationChannel(channel)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val uiFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, InCallActivity::class.java).addFlags(uiFlags), flags,
        )
        val answer = PendingIntent.getActivity(
            this, 1,
            Intent(this, InCallActivity::class.java).setAction(InCallActivity.ACTION_ANSWER).addFlags(uiFlags),
            flags,
        )
        val decline = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_DECLINE),
            flags,
        )
        val person = Person.Builder().setName(name).setImportant(true).build()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setStyle(Notification.CallStyle.forIncomingCall(person, decline, answer))
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "incoming_calls"
        const val NOTIFICATION_ID = 4101
    }
}
