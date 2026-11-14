package com.iosphone.app.ui.incall

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.CallLook
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassMenu
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.MenuItem
import com.iosphone.app.ui.common.PadKey
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.ui.theme.ltr
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

enum class CallMode { Ringing, Pad, Controls }

class OtherCall(val label: String, val onClick: () -> Unit)

class CallUi(
    val name: String,
    val number: String,
    val photo: String?,
    val statusLabel: String?,
    val connectTime: Long,
    val sim: String?,
    val mode: CallMode,
    val typed: String,
    val speakerOn: Boolean,
    val muted: Boolean,
    val onHold: Boolean,
    val finished: Boolean,
    val others: List<OtherCall>,
    val canMerge: Boolean,
)

class CallActions(
    val decline: () -> Unit = {},
    val accept: () -> Unit = {},
    val remind: () -> Unit = {},
    val message: () -> Unit = {},
    val speaker: () -> Unit = {},
    val mute: () -> Unit = {},
    val hold: () -> Unit = {},
    val addCall: () -> Unit = {},
    val merge: () -> Unit = {},
    val keypad: () -> Unit = {},
    val hidePad: () -> Unit = {},
    val end: () -> Unit = {},
    val digit: (String) -> Unit = {},
)

private val padKeys = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to ""),
)

private fun secondsSince(from: Long): Long = maxOf(0L, (System.currentTimeMillis() - from) / 1000)

@Composable
private fun StatusText(label: String?, connectTime: Long, style: TextStyle) {
    if (label != null) {
        BasicText(label, style = style)
    } else {
        val seconds by produceState(secondsSince(connectTime), connectTime) {
            while (true) {
                value = secondsSince(connectTime)
                delay(1000L - (System.currentTimeMillis() - connectTime).mod(1000L))
            }
        }
        BasicText(Formats.timer(seconds), style = style)
    }
}

@Composable
fun CallContent(ui: CallUi, act: CallActions, look: CallLook, p: CallPalette, insets: Boolean, modifier: Modifier = Modifier) {
    var moreMenu by remember { mutableStateOf(false) }
    val mode = ui.mode
    val ringing = mode == CallMode.Ringing
    Column(
        modifier
            .fillMaxSize()
            .then(if (insets) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        if (ui.others.isNotEmpty() && !ringing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.others.forEach { other ->
                    Box(
                        Modifier
                            .pressable(pressedScale = 0.96f) { other.onClick() }
                            .background(p.fg.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        BasicText(
                            other.label + " · " + stringResource(R.string.on_hold),
                            style = IosType.callout.copy(color = p.fg),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        StatusText(
            ui.statusLabel,
            ui.connectTime,
            IosType.headline.copy(color = p.fgSoft, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            ui.name,
            style = IosType.largeTitle.copy(color = p.fg, textAlign = TextAlign.Center, fontSize = (26f + 18f * look.nameSize).sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (look.showNumber && ui.number.isNotBlank() && PhoneFormat.format(ui.number) != ui.name) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                PhoneFormat.format(ui.number),
                style = IosType.callout.copy(color = p.fgSoft, textAlign = TextAlign.Center).ltr(),
                maxLines = 1,
            )
        }
        if (look.showSim && !ui.sim.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            BasicText(ui.sim, style = IosType.footnote.copy(color = p.fgSoft, textAlign = TextAlign.Center), maxLines = 1)
        }
        if (ringing || mode == CallMode.Pad) Spacer(Modifier.height(10.dp))
        if (mode == CallMode.Pad) {
            BasicText(
                ui.typed,
                style = IosType.title1.copy(color = p.fg, textAlign = TextAlign.Center).ltr(),
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
                        CallAvatar(ui.name, ui.photo, look, p, true)
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            SmallAction(Icons.Filled.Alarm, stringResource(R.string.remind_me), p, act.remind)
                            SmallAction(IosIcons.Message, stringResource(R.string.message), p, act.message)
                        }
                        Spacer(Modifier.height(36.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            BigAction(IosIcons.CallEnd, stringResource(R.string.decline), p.decline, p, act.decline)
                            BigAction(IosIcons.Phone, stringResource(R.string.accept), p.accept, p, act.accept)
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
                                            fill = p.pad,
                                            pressedFill = p.padPressed,
                                            content = p.fg,
                                            onPress = { act.digit(digit) },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            EndButton(p, 76, act.end)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                BasicText(
                                    stringResource(R.string.hide),
                                    style = IosType.headline.copy(color = p.fg),
                                    modifier = Modifier.pressable { act.hidePad() }.padding(12.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                    CallMode.Controls -> {
                        if (look.avatarActive) {
                            CallAvatar(ui.name, ui.photo, look, p, false)
                            Spacer(Modifier.weight(1f))
                        }
                        Box(Modifier.alpha(if (ui.finished) 0.4f else 1f)) {
                            Column(verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                    CallControl(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.speaker), ui.speakerOn, p, onClick = act.speaker)
                                    CallControl(
                                        if (ui.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                        stringResource(R.string.mute),
                                        ui.muted,
                                        p,
                                        onClick = act.mute,
                                    )
                                    CallControl(Icons.Filled.Dialpad, stringResource(R.string.keypad), false, p, onClick = act.keypad)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                    CallControl(
                                        if (ui.onHold) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        stringResource(if (ui.onHold) R.string.resume else R.string.hold),
                                        ui.onHold,
                                        p,
                                        onClick = act.hold,
                                    )
                                    CallControl(Icons.Filled.PersonAdd, stringResource(R.string.add_call), false, p, onClick = act.addCall)
                                    CallControl(IosIcons.Message, stringResource(R.string.message), false, p, onClick = act.message)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                                    if (ui.canMerge) {
                                        Box {
                                            CallControl(Icons.Filled.MoreHoriz, stringResource(R.string.more), moreMenu, p) { moreMenu = true }
                                            GlassMenu(
                                                expanded = moreMenu,
                                                onDismiss = { moreMenu = false },
                                                alignment = Alignment.TopStart,
                                                offsetY = (-90).dp,
                                                width = 230.dp,
                                                items = listOf(MenuItem(stringResource(R.string.merge)) { act.merge() }),
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        EndButton(p, 76, act.end)
                                        Spacer(Modifier.height(8.dp))
                                        BasicText(stringResource(R.string.end), style = IosType.caption.copy(color = p.fg))
                                    }
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

@Composable
fun CallAvatar(name: String, photo: String?, look: CallLook, p: CallPalette, pulsing: Boolean) {
    val avatarDp = (96f + look.avatarSize * 84f).dp
    val shape = avatarShapeOf(look.avatarShape)
    val phase: State<Float>? = if (pulsing && look.ring != 2) {
        rememberInfiniteTransition(label = "ring").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1900, easing = LinearOutSlowInEasing), RepeatMode.Restart),
            label = "ringPhase",
        )
    } else {
        null
    }
    Box(Modifier.size(avatarDp), contentAlignment = Alignment.Center) {
        if (phase != null) {
            if (look.ring == 0) {
                Box(
                    Modifier
                        .size(avatarDp)
                        .graphicsLayer {
                            val r = phase.value
                            scaleX = 1f + r * 0.7f
                            scaleY = 1f + r * 0.7f
                            alpha = (1f - r) * 0.35f
                        }
                        .background(p.accent, shape),
                )
            } else {
                Box(
                    Modifier
                        .requiredSize(avatarDp * 1.9f)
                        .drawBehind {
                            val k = 0.75f + 0.25f * sin(2f * PI.toFloat() * phase.value)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(p.accent.copy(alpha = 0.55f * k), p.accent.copy(alpha = 0f)),
                                    Offset(this.size.width / 2f, this.size.height / 2f),
                                    this.size.minDimension / 2f,
                                ),
                            )
                        },
                )
            }
        }
        Avatar(name, photo, avatarDp, shape = shape)
    }
}

@Composable
fun EndButton(p: CallPalette, sizeDp: Int, onClick: () -> Unit) {
    ActionButton(IosIcons.CallEnd, p.decline, p, sizeDp, onClick)
}

@Composable
private fun ActionButton(icon: ImageVector, color: Color, p: CallPalette, sizeDp: Int, onClick: () -> Unit) {
    val outline = p.style == 2
    Box(
        Modifier
            .size(sizeDp.dp)
            .pressable(pressedScale = 0.92f, onClick = onClick)
            .then(if (p.style == 1) Modifier.shadow(14.dp, p.shape, clip = false, ambientColor = color, spotColor = color) else Modifier)
            .then(if (outline) Modifier.border(2.dp, color, p.shape) else Modifier.background(color, p.shape)),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size((sizeDp * 0.45f).dp), colorFilter = ColorFilter.tint(if (outline) color else Color.White))
    }
}

@Composable
fun CallControl(
    icon: ImageVector,
    label: String,
    active: Boolean,
    p: CallPalette,
    enabled: Boolean = true,
    activeTint: Color? = null,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (enabled) 1f else 0.4f)) {
        Box(
            Modifier
                .size(76.dp)
                .pressable(enabled = enabled, pressedScale = 0.92f, onClick = onClick)
                .then(if (p.style == 2 && !active) Modifier.border(1.5.dp, p.fg.copy(alpha = 0.55f), p.shape) else Modifier)
                .background(if (active) p.ctrlOn else p.ctrl, p.shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                icon,
                null,
                Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(if (active) (activeTint ?: p.ctrlOnIcon) else p.fg),
            )
        }
        Spacer(Modifier.height(8.dp))
        BasicText(label, style = IosType.caption.copy(color = p.fg))
    }
}

@Composable
fun SmallAction(icon: ImageVector, label: String, p: CallPalette, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .pressable(pressedScale = 0.92f, onClick = onClick)
                .then(if (p.style == 2) Modifier.border(1.5.dp, p.fg.copy(alpha = 0.55f), p.shape) else Modifier)
                .background(if (p.style == 2) Color.Transparent else if (p.style == 1) p.accent.copy(alpha = 0.3f) else p.fg.copy(alpha = 0.18f), p.shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(p.fg))
        }
        Spacer(Modifier.height(6.dp))
        BasicText(label, style = IosType.caption.copy(color = p.fg))
    }
}

@Composable
fun BigAction(icon: ImageVector, label: String, color: Color, p: CallPalette, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ActionButton(icon, color, p, 80, onClick)
        Spacer(Modifier.height(8.dp))
        BasicText(label, style = IosType.caption.copy(color = p.fg))
    }
}
