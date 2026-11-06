package com.iosphone.app.ui.incall

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.iosphone.app.data.AppSettings
import com.iosphone.app.ui.common.CallBackdrop
import com.iosphone.app.ui.common.PadKey
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.telecom.CallManager
import com.iosphone.app.telecom.ContactLookup
import com.iosphone.app.telecom.LookupInfo
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassMenu
import com.iosphone.app.ui.common.MenuItem
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private class Shown(
    val name: String,
    val number: String,
    val photo: String?,
    val state: Int,
    val connectTime: Long,
    val incoming: Boolean,
)

private val padKeys = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to ""),
)

private val callRed = Color(0xFFFF3B30)
private val callGreen = Color(0xFF34C759)

@Suppress("DEPRECATION")
@Composable
fun InCallScreen(onFinish: () -> Unit, onAddCall: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val tick = CallManager.tick
    val primary = CallManager.primary()
    val others = CallManager.calls.filter { it !== primary }
    val info = if (primary != null) CallManager.info(primary) else null
    val lookup by produceState(LookupInfo(null, null), info?.number) {
        val n = info?.number.orEmpty()
        value = withContext(Dispatchers.IO) { ContactLookup.find(context, n) }
    }
    val holder = remember { arrayOfNulls<Shown>(1) }
    if (info != null) {
        val label = lookup.name ?: info.displayName ?: PhoneFormat.format(info.number).ifEmpty { stringResource(R.string.unknown) }
        holder[0] = Shown(label, info.number, lookup.photo, info.state, info.connectTime, info.incoming)
    }
    val shown = holder[0]
    val finished = CallManager.calls.isEmpty()

    LaunchedEffect(finished, shown == null) {
        if (finished) {
            delay(if (shown == null) 0 else 900)
            onFinish()
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    var showPad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var moreMenu by remember { mutableStateOf(false) }

    val ringingNow = shown != null && shown.state == Call.STATE_RINGING && !finished

    Box(Modifier.fillMaxSize().background(Color(0xFF141A25))) {
        // User-chosen background (Settings), blurred with BlurView. Cross-fades when a ringing call is answered.
        Crossfade(targetState = ringingNow, animationSpec = tween(450), label = "callBackdrop") { r ->
            CallBackdrop(if (r) AppSettings.incoming else AppSettings.active, Modifier.fillMaxSize())
        }
        if (shown == null) return@Box
        val ringing = shown.state == Call.STATE_RINGING && !finished
        val onHold = shown.state == Call.STATE_HOLDING
        val dialing = shown.state == Call.STATE_DIALING || shown.state == Call.STATE_CONNECTING ||
            shown.state == Call.STATE_NEW || shown.state == Call.STATE_SELECT_PHONE_ACCOUNT
        val statusText = when {
            finished -> stringResource(R.string.call_ended)
            ringing -> stringResource(R.string.mobile)
            dialing -> stringResource(R.string.calling)
            onHold -> stringResource(R.string.on_hold)
            shown.connectTime > 0 -> Formats.timer(maxOf(0L, (now - shown.connectTime) / 1000))
            else -> stringResource(R.string.calling)
        }
        val speakerOn = CallManager.route == CallAudioState.ROUTE_SPEAKER
        val muted = CallManager.muted
        val mode = when {
            ringing -> CallMode.Ringing
            showPad -> CallMode.Pad
            else -> CallMode.Controls
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            if (others.isNotEmpty() && !ringing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { other ->
                        val oi = CallManager.info(other)
                        val oname = oi.displayName ?: PhoneFormat.format(oi.number)
                        Box(
                            Modifier
                                .pressable(pressedScale = 0.96f) { CallManager.swapTo(other) }
                                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            BasicText(
                                oname + " · " + stringResource(R.string.on_hold),
                                style = IosType.callout.copy(color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            BasicText(
                statusText,
                style = IosType.headline.copy(color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(6.dp))
            BasicText(
                shown.name,
                style = IosType.largeTitle.copy(color = Color.White, textAlign = TextAlign.Center, fontSize = 32.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (ringing || showPad) {
                Spacer(Modifier.height(10.dp))
            }
            if (showPad && !ringing) {
                BasicText(
                    typed,
                    style = IosType.title1.copy(color = Color.White, textAlign = TextAlign.Center),
                    maxLines = 1,
                    modifier = Modifier.height(40.dp),
                )
            }
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.94f)) togetherWith fadeOut(tween(140))
                },
                label = "callMode",
            ) { m ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    when (m) {
                        CallMode.Ringing -> {
                            PulsingAvatar(shown.name, shown.photo)
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                                SmallAction(Icons.Filled.Alarm, stringResource(R.string.remind_me)) {
                                    CallManager.primary()?.let { CallManager.reject(it) }
                                }
                                SmallAction(Icons.Filled.ChatBubble, stringResource(R.string.message)) {
                                    val number = shown.number
                                    CallManager.primary()?.let { CallManager.reject(it) }
                                    onMessage(number)
                                }
                            }
                            Spacer(Modifier.height(36.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                BigAction(Icons.Filled.CallEnd, stringResource(R.string.decline), callRed) {
                                    CallManager.primary()?.let { CallManager.reject(it) }
                                }
                                BigAction(Icons.Filled.Call, stringResource(R.string.accept), callGreen) {
                                    CallManager.primary()?.let { CallManager.answer(it) }
                                }
                            }
                            Spacer(Modifier.height(36.dp))
                        }
                        CallMode.Pad -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                padKeys.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                                        row.forEach { (digit, letters) ->
                                            PadKey(
                                                digit = digit,
                                                letters = letters,
                                                size = 76.dp,
                                                fill = Color.White.copy(alpha = 0.18f),
                                                pressedFill = Color.White.copy(alpha = 0.42f),
                                                content = Color.White,
                                                onTap = {
                                                    typed += digit
                                                    CallManager.primary()?.let { CallManager.dtmf(it, digit[0]) }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.size(76.dp).pressable { CallManager.primary()?.let { CallManager.hangup(it) } }.background(callRed, CircleShape), contentAlignment = Alignment.Center) {
                                    Image(Icons.Filled.CallEnd, null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(Color.White))
                                }
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    BasicText(
                                        stringResource(R.string.hide),
                                        style = IosType.headline.copy(color = Color.White),
                                        modifier = Modifier.pressable { showPad = false }.padding(12.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(28.dp))
                        }
                        CallMode.Controls -> {
                            Box(Modifier.alpha(if (finished) 0.4f else 1f)) {
                                Column(verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                        CallControl(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speaker), speakerOn) {
                                            CallManager.setSpeaker(!speakerOn)
                                        }
                                        CallControl(Icons.Filled.Videocam, stringResource(R.string.facetime), false, enabled = false) { }
                                        CallControl(
                                            if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                            stringResource(R.string.mute),
                                            muted,
                                        ) { CallManager.applyMute(!muted) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                        Box {
                                            CallControl(Icons.Filled.MoreHoriz, stringResource(R.string.more), moreMenu) { moreMenu = true }
                                            GlassMenu(
                                                expanded = moreMenu,
                                                onDismiss = { moreMenu = false },
                                                alignment = Alignment.TopStart,
                                                offsetY = (-170).dp,
                                                width = 230.dp,
                                                items = buildList {
                                                    add(
                                                        MenuItem(stringResource(if (onHold) R.string.resume else R.string.hold)) {
                                                            CallManager.primary()?.let { if (onHold) CallManager.unhold(it) else CallManager.hold(it) }
                                                        },
                                                    )
                                                    add(MenuItem(stringResource(R.string.add_call)) { onAddCall() })
                                                    if (others.isNotEmpty()) {
                                                        add(
                                                            MenuItem(stringResource(R.string.merge)) {
                                                                CallManager.primary()?.let { p -> CallManager.merge(p, others.first()) }
                                                            },
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                Modifier.size(76.dp).pressable(pressedScale = 0.92f) {
                                                    CallManager.primary()?.let { CallManager.hangup(it) }
                                                }.background(callRed, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Image(Icons.Filled.CallEnd, null, Modifier.size(34.dp), colorFilter = ColorFilter.tint(Color.White))
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            BasicText(stringResource(R.string.end), style = IosType.caption.copy(color = Color.White))
                                        }
                                        CallControl(Icons.Filled.Dialpad, stringResource(R.string.keypad), false) { showPad = true }
                                    }
                                }
                            }
                            Spacer(Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

private enum class CallMode { Ringing, Pad, Controls }

/** Incoming-call avatar with a soft expanding ring, like a heartbeat. */
@Composable
private fun PulsingAvatar(name: String, photo: String?) {
    val transition = rememberInfiniteTransition(label = "ringPulse")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(132.dp)
                .graphicsLayer {
                    scaleX = 1f + ring * 0.7f
                    scaleY = 1f + ring * 0.7f
                    alpha = (1f - ring) * 0.35f
                }
                .background(Color.White, CircleShape),
        )
        Avatar(name, photo, 132.dp)
    }
}

@Composable
private fun CallControl(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (enabled) 1f else 0.4f)) {
        Box(
            Modifier
                .size(76.dp)
                .pressable(enabled = enabled, pressedScale = 0.92f, onClick = onClick)
                .background(if (active) Color.White else Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                icon,
                null,
                Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(if (active) Color(0xFF1C1C1E) else Color.White),
            )
        }
        Spacer(Modifier.height(8.dp))
        BasicText(label, style = IosType.caption.copy(color = Color.White))
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).pressable(pressedScale = 0.92f, onClick = onClick).background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        Spacer(Modifier.height(6.dp))
        BasicText(label, style = IosType.caption.copy(color = Color.White))
    }
}

@Composable
private fun BigAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(80.dp).pressable(pressedScale = 0.92f, onClick = onClick).background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(icon, null, Modifier.size(36.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        Spacer(Modifier.height(8.dp))
        BasicText(label, style = IosType.caption.copy(color = Color.White))
    }
}
