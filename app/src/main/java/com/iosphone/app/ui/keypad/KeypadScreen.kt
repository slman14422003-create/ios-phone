package com.iosphone.app.ui.keypad

import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.LocalAppData
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
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun press(digit: String) {
        tick()
        runCatching { tones?.startTone(toneFor(digit), 120) }
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

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (raw.isNotEmpty()) {
                Image(
                    Icons.Filled.PersonAdd,
                    null,
                    Modifier.size(34.dp).pressable { nav.push(Route.Editor(null, raw)) },
                    colorFilter = ColorFilter.tint(colors.label),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
            BasicText(
                text = PhoneFormat.format(raw),
                style = IosType.dialedNumber.copy(color = colors.label, textAlign = TextAlign.Center),
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
            if (suggestion != null) {
                Row(
                    Modifier
                        .pressable(pressedScale = 0.97f) { raw = PhoneFormat.digits(suggestion.phone.number) }
                        .glass(RoundedCornerShape(26.dp))
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(Icons.Filled.AccountCircle, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.label))
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        suggestion.contact.name,
                        style = IosType.body.copy(color = colors.secondaryLabel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        highlighted(
                            PhoneFormat.format(suggestion.phone.number),
                            suggestion.matchStart,
                            suggestion.matchLength,
                            colors.label,
                            colors.secondaryLabel,
                        ),
                        style = IosType.body,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { key ->
                        KeypadKey(
                            key = key,
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
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.size(80.dp))
            Spacer(Modifier.size(22.dp))
            CallButton(onTap = ::onCall)
            Spacer(Modifier.size(22.dp))
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                if (raw.isNotEmpty()) {
                    DeleteButton(
                        onDelete = {
                            tick()
                            raw = raw.dropLast(1)
                        },
                        launchJob = { block -> scope.launch { block() } },
                    )
                }
            }
        }
        Spacer(Modifier.height(112.dp))
    }
}

@Composable
private fun KeypadKey(key: Key, onTap: () -> Unit, onLong: (() -> Unit)?) {
    val colors = Ios.colors
    var pressed by remember { mutableStateOf(false) }
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(80.dp)
            .shadow(
                elevation = if (pressed) 2.dp else 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (colors.dark) 0.3f else 0.05f),
                spotColor = Color.Black.copy(alpha = if (colors.dark) 0.4f else 0.12f),
            )
            .clip(shape)
            .background(if (pressed) colors.selection else colors.keyFill)
            .pointerInput(onLong) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = onLong?.let { l -> { _ -> l() } },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(key.digit, style = IosType.keypadDigit.copy(color = colors.label))
            if (key.letters.isNotEmpty()) {
                BasicText(key.letters, style = IosType.keypadLetters.copy(color = colors.label))
            }
        }
    }
}

@Composable
private fun CallButton(onTap: () -> Unit) {
    val colors = Ios.colors
    Box(
        modifier = Modifier
            .size(80.dp)
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
private fun DeleteButton(onDelete: () -> Unit, launchJob: (suspend () -> Unit) -> Job) {
    val colors = Ios.colors
    Box(
        modifier = Modifier
            .size(80.dp)
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
