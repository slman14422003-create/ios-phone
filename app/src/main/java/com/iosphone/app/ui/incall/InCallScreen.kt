package com.iosphone.app.ui.incall

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.telecom.CallManager
import com.iosphone.app.telecom.CallRecorder
import com.iosphone.app.telecom.CallReminder
import com.iosphone.app.telecom.ContactLookup
import com.iosphone.app.telecom.LookupInfo
import com.iosphone.app.telecom.QuickReply
import com.iosphone.app.telecom.SimManager
import com.iosphone.app.ui.common.CallBackdrop
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private class Shown(
    val call: Call,
    val name: String,
    val number: String,
    val state: Int,
    val connectTime: Long,
)

@Suppress("DEPRECATION")
@Composable
fun InCallScreen(onFinish: () -> Unit, onAddCall: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val tick = CallManager.tick
    val ringingCall = CallManager.ringing()
    val ongoingCall = CallManager.ongoing()
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(ringingCall) { expanded = false }
    val bannerMode = AppSettings.incomingDisplay == 0 && ringingCall != null && ongoingCall != null && !expanded
    val primary = if (bannerMode) ongoingCall else CallManager.primary()
    val others = CallManager.calls.filter { it !== primary && !(bannerMode && it === ringingCall) }
    val ringInfo = if (bannerMode && ringingCall != null) CallManager.info(ringingCall) else null
    val ringLookup by produceState(LookupInfo(null, null), ringInfo?.number) {
        val n = ringInfo?.number.orEmpty()
        value = if (n.isEmpty()) LookupInfo(null, null) else withContext(Dispatchers.IO) { ContactLookup.find(context, n) }
    }
    val info = if (primary != null) CallManager.info(primary) else null
    val lookup by produceState(LookupInfo(null, null), info?.number) {
        val n = info?.number.orEmpty()
        value = withContext(Dispatchers.IO) { ContactLookup.find(context, n) }
    }
    val sims = remember { SimManager.list(context) }
    val holder = remember { arrayOfNulls<Shown>(1) }
    if (info != null) {
        val label = lookup.name ?: info.displayName ?: PhoneFormat.format(info.number).ifEmpty { stringResource(R.string.unknown) }
        holder[0] = Shown(info.call, label, info.number, info.state, info.connectTime)
    }
    val shown = holder[0]
    val finished = CallManager.calls.isEmpty()

    LaunchedEffect(finished, shown == null) {
        if (finished) {
            delay(if (shown == null) 0 else 900)
            onFinish()
        }
    }

    var showPad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var sheet by remember { mutableIntStateOf(0) }
    BackHandler(enabled = sheet != 0) { sheet = 0 }

    val ringingNow = shown != null && shown.state == Call.STATE_RINGING && !finished
    val cfg = if (ringingNow) AppSettings.incoming else AppSettings.active
    val look = cfg.look
    val palette = remember(look) { callPalette(look) }
    val photo = lookup.fullPhoto ?: lookup.photo

    Box(Modifier.fillMaxSize().background(Color(0xFF141A25))) {
        Crossfade(targetState = ringingNow, animationSpec = tween(450), label = "callBackdrop") { r ->
            val c = if (r) AppSettings.incoming else AppSettings.active
            CallBackdrop(c.look, c.file(context), photo, Modifier.fillMaxSize())
        }
        if (shown == null) return@Box
        val ringing = shown.state == Call.STATE_RINGING && !finished
        val onHold = shown.state == Call.STATE_HOLDING
        val dialing = shown.state == Call.STATE_DIALING || shown.state == Call.STATE_CONNECTING ||
            shown.state == Call.STATE_NEW || shown.state == Call.STATE_SELECT_PHONE_ACCOUNT
        val statusLabel: String? = when {
            finished -> stringResource(R.string.call_ended)
            ringing -> stringResource(R.string.mobile)
            dialing -> stringResource(R.string.calling)
            onHold -> stringResource(R.string.on_hold)
            shown.connectTime > 0 -> null
            else -> stringResource(R.string.calling)
        }
        val mode = when {
            ringing -> CallMode.Ringing
            showPad -> CallMode.Pad
            else -> CallMode.Controls
        }
        val simLabel: String? = if (sims.size > 1) {
            val handle = shown.call.details?.accountHandle
            val sim = sims.firstOrNull { it.handle == handle }
            if (sim == null) null else stringResource(R.string.sim_n, sim.slot + 1) + if (sim.label.isNotBlank()) " · " + sim.label else ""
        } else {
            null
        }
        val speakerOn = CallManager.route == CallAudioState.ROUTE_SPEAKER
        val muted = CallManager.muted
        val ui = CallUi(
            name = shown.name,
            number = shown.number,
            photo = lookup.photo,
            statusLabel = statusLabel,
            connectTime = shown.connectTime,
            sim = simLabel,
            mode = mode,
            typed = typed,
            speakerOn = speakerOn,
            muted = muted,
            recording = CallRecorder.recording,
            recordEnabled = CallRecorder.recording || shown.state == Call.STATE_ACTIVE,
            onHold = onHold,
            finished = finished,
            others = others.map { other ->
                val oi = CallManager.info(other)
                OtherCall(oi.displayName ?: PhoneFormat.format(oi.number)) { CallManager.swapTo(other) }
            },
            canMerge = others.isNotEmpty(),
        )
        val act = CallActions(
            decline = { (CallManager.ringing() ?: primary)?.let { CallManager.reject(it) } },
            accept = { (CallManager.ringing() ?: primary)?.let { CallManager.answer(it) } },
            remind = { sheet = 2 },
            message = { sheet = 1 },
            speaker = { CallManager.setSpeaker(!speakerOn) },
            record = { primary?.let { CallRecorder.toggle(context, it) } },
            mute = { CallManager.applyMute(!muted) },
            hold = { primary?.let { if (onHold) CallManager.unhold(it) else CallManager.hold(it) } },
            addCall = onAddCall,
            merge = { primary?.let { p -> others.firstOrNull()?.let { o -> CallManager.merge(p, o) } } },
            keypad = { showPad = true },
            hidePad = { showPad = false },
            end = { primary?.let { CallManager.hangup(it) } },
            digit = { d ->
                if (typed.length < 40) typed += d
                primary?.let { CallManager.dtmf(it, d[0]) }
            },
        )
        CallContent(ui, act, look, palette, insets = true)
        IncomingBanner(
            visible = bannerMode,
            name = ringLookup.name ?: ringInfo?.displayName ?: PhoneFormat.format(ringInfo?.number.orEmpty()).ifEmpty { stringResource(R.string.unknown) },
            number = ringInfo?.number.orEmpty(),
            photo = ringLookup.photo,
            decline = palette.decline,
            accept = palette.accept,
            onExpand = { expanded = true },
            onDecline = { ringingCall?.let { CallManager.reject(it) } },
            onAccept = { ringingCall?.let { CallManager.answer(it) } },
        )

        val replies = AppSettings.quickReplies ?: listOf(
            stringResource(R.string.qr_1),
            stringResource(R.string.qr_2),
            stringResource(R.string.qr_3),
            stringResource(R.string.qr_4),
        )
        val customLabel = stringResource(R.string.qr_custom)
        val replyItems = remember(replies, customLabel, shown.number) {
            replies.map { text ->
                text to {
                    CallManager.ringing()?.let { CallManager.reject(it) }
                    QuickReply.send(context, shown.number, text)
                }
            } + (customLabel to {
                CallManager.ringing()?.let { CallManager.reject(it) }
                onMessage(shown.number)
            })
        }
        val remindItems = listOf(5, 15, 60).map { minutes ->
            val label = if (minutes < 60) stringResource(R.string.remind_in_min, minutes) else stringResource(R.string.remind_in_hour)
            label to {
                CallManager.ringing()?.let { CallManager.reject(it) }
                CallReminder.schedule(context, shown.number, shown.name, minutes)
            }
        }
        CallSheet(
            visible = sheet != 0 && ringing,
            title = stringResource(if (sheet == 2) R.string.remind_title else R.string.quick_reply_title),
            items = if (sheet == 2) remindItems else replyItems,
            onDismiss = { sheet = 0 },
        )
    }
}

@Composable
private fun CallSheet(visible: Boolean, title: String, items: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xF21C1C1E), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .navigationBarsPadding()
                    .padding(top = 18.dp, bottom = 12.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                BasicText(
                    title,
                    style = IosType.footnote.copy(color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
                items.forEach { (label, action) ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .pressable(pressedScale = 0.98f) {
                                onDismiss()
                                action()
                            }
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicText(label, style = IosType.body.copy(color = Color.White), maxLines = 2)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
