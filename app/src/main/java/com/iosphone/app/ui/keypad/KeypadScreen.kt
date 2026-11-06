package com.iosphone.app.ui.keypad

import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.PadKey
import com.iosphone.app.domain.DialSuggest
import com.iosphone.app.domain.Suggestion
import com.iosphone.app.ui.common.GlassMenu
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.MenuItem
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.VOICEMAIL
import com.iosphone.app.ui.common.glass
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Key(val digit: String, val letters: String)

private val keys = listOf(
    listOf(Key("1", ""), Key("2", "ABC"), Key("3", "DEF")),
    listOf(Key("4", "GHI"), Key("5", "JKL"), Key("6", "MNO")),
    listOf(Key("7", "PQRS"), Key("8", "TUV"), Key("9", "WXYZ")),
    listOf(Key("*", ""), Key("0", "+"), Key("#", "")),
)

private fun toneFor(digit: String): Int = when (digit) {
    "*" -> ToneGenerator.TONE_DTMF_S
    "#" -> ToneGenerator.TONE_DTMF_P
    else -> digit[0] - '0'
}

private fun highlighted(number: String, start: Int, length: Int, bold: Color, normal: Color): AnnotatedString {
    var seen = 0
    return buildAnnotatedString {
        for (ch in number) {
            if (ch.isDigit()) {
                val hit = start >= 0 && seen >= start && seen < start + length
                withStyle(SpanStyle(color = if (hit) bold else normal, fontWeight = if (hit) FontWeight.SemiBold else FontWeight.Normal)) {
                    append(ch)
                }
                seen++
            } else {
                withStyle(SpanStyle(color = normal)) { append(ch) }
            }
        }
    }
}

@Composable
fun KeypadScreen(initialNumber: String?) {
    val colors = Ios.colors
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val data = LocalAppData.current
    val nav = LocalNav.current
    var raw by rememberSaveable { mutableStateOf("") }
    var pasteMenu by remember { mutableStateOf(false) }
    val caller = rememberCaller()
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tones?.release() } }
    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrEmpty()) raw = PhoneFormat.digits(initialNumber)
    }
    val suggestion: Suggestion? = remember(raw, data.contacts) { DialSuggest.find(data.contacts, raw) }

    fun tick() {
        if (AppSettings.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun press(digit: String) {
        tick()
        if (AppSettings.keySounds) runCatching { tones?.startTone(toneFor(digit), 120) }
        raw += digit
    }

    fun onCall() {
        tick()
        if (raw.isEmpty()) {
            val last = data.calls.firstOrNull { it.isOutgoing }
            if (last != null) raw = PhoneFormat.digits(last.number)
        } else {
            caller(raw)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        val gap = 20.dp
        val keySize = minOf(84.dp, maxOf(64.dp, (maxWidth - 88.dp - gap * 2) / 3))
        val rowGap = if (maxHeight < 640.dp) 10.dp else 14.dp
        val enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.7f)
        val exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f)

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(visible = raw.isNotEmpty(), enter = enter, exit = exit) {
                    Image(
                        Icons.Filled.PersonAdd,
                        null,
                        Modifier.size(34.dp).pressable { nav.push(Route.Editor(null, raw)) },
                        colorFilter = ColorFilter.tint(colors.label),
                    )
                }
            }
            val shownNumber = PhoneFormat.format(raw)
            val numberSize = when {
                shownNumber.length <= 11 -> 44.sp
                shownNumber.length <= 14 -> 38.sp
                shownNumber.length <= 17 -> 32.sp
                else -> 26.sp
            }
            Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                BasicText(
                    text = shownNumber,
                    style = IosType.dialedNumber.copy(color = colors.label, textAlign = TextAlign.Center, fontSize = numberSize),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).pointerInput(Unit) {
                        detectTapGestures(onLongPress = { pasteMenu = true })
                    },
                )
                GlassMenu(
                    expanded = pasteMenu,
                    onDismiss = { pasteMenu = false },
                    alignment = Alignment.BottomCenter,
                    offsetY = 8.dp,
                    width = 160.dp,
                    items = listOf(
                        MenuItem(stringResource(R.string.paste)) {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            val digits = PhoneFormat.digits(text)
                            if (digits.isNotEmpty()) raw = digits
                        },
                    ),
                )
            }
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = suggestion,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(110)) },
                    label = "suggestion",
                ) { s ->
                    if (s != null) {
                        Row(
                            Modifier
                                .pressable(pressedScale = 0.97f) { raw = PhoneFormat.digits(s.phone.number) }
                                .glass(RoundedCornerShape(26.dp))
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(Icons.Filled.AccountCircle, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.label))
                            Spacer(Modifier.width(10.dp))
                            BasicText(
                                s.contact.name,
                                style = IosType.body.copy(color = colors.secondaryLabel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                highlighted(
                                    PhoneFormat.format(s.phone.number),
                                    s.matchStart,
                                    s.matchLength,
                                    colors.label,
                                    colors.secondaryLabel,
                                ),
                                style = IosType.body,
                                maxLines = 1,
                            )
                        }
                    } else {
                        Spacer(Modifier.size(1.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { key ->
                            PadKey(
                                digit = key.digit,
                                letters = key.letters,
                                size = keySize,
                                fill = colors.keyFill,
                                pressedFill = colors.keyPressed,
                                content = colors.label,
                                onTap = { press(key.digit) },
                                onLong = when (key.digit) {
                                    "0" -> {
                                        {
                                            tick()
                                            raw = raw.dropLast(1) + "+"
                                        }
                                    }
                                    "1" -> {
                                        {
                                            tick()
                                            caller(VOICEMAIL)
                                        }
                                    }
                                    else -> null
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.size(keySize))
                Spacer(Modifier.size(gap))
                CallButton(keySize, onTap = ::onCall)
                Spacer(Modifier.size(gap))
                DeleteSlot(
                    visible = raw.isNotEmpty(),
                    size = keySize,
                    onDelete = {
                        tick()
                        raw = raw.dropLast(1)
                    },
                    launchJob = { block -> scope.launch { block() } },
                )
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun CallButton(size: Dp, onTap: () -> Unit) {
    val colors = Ios.colors
    Box(
        modifier = Modifier
            .size(size)
            .pressable(pressedScale = 0.92f, onClick = onTap)
            .clip(CircleShape)
            .background(colors.green),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun DeleteButton(size: Dp, onDelete: () -> Unit, launchJob: (suspend () -> Unit) -> Job) {
    val colors = Ios.colors
    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDelete()
                        val job = launchJob {
                            delay(450)
                            while (true) {
                                onDelete()
                                delay(70)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.width(46.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.label),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DeleteSlot(visible: Boolean, size: Dp, onDelete: () -> Unit, launchJob: (suspend () -> Unit) -> Job) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
        ) {
            DeleteButton(size = size, onDelete = onDelete, launchJob = launchJob)
        }
    }
}
