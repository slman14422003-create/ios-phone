package com.iosphone.app.ui.keypad

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.iosphone.app.ui.theme.ltr
import android.content.ClipboardManager
import com.iosphone.app.telecom.Ussd
import com.iosphone.app.telecom.DialCodes
import androidx.compose.ui.window.Dialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.iosphone.app.util.KeyFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.iosphone.app.telecom.SimManager
import androidx.compose.material.icons.filled.SimCard
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.PadKey
import com.iosphone.app.domain.DialSuggest
import com.iosphone.app.domain.Suggestion
import com.iosphone.app.ui.common.GlassIconButton
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
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Key(val digit: String, val letters: String)

private val keys = listOf(
    listOf(Key("1", ""), Key("2", "ABC"), Key("3", "DEF")),
    listOf(Key("4", "GHI"), Key("5", "JKL"), Key("6", "MNO")),
    listOf(Key("7", "PQRS"), Key("8", "TUV"), Key("9", "WXYZ")),
    listOf(Key("*", ""), Key("0", "+"), Key("#", "")),
)

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
    val scope = rememberCoroutineScope()
    val data = LocalAppData.current
    val nav = LocalNav.current
    var raw by rememberSaveable { mutableStateOf("") }
    var pasteMenu by remember { mutableStateOf(false) }
    val sims = remember(data.version) { SimManager.list(context) }
    var simId by rememberSaveable { mutableStateOf("") }
    var simMenu by remember { mutableStateOf(false) }
    val activeSim = remember(sims, simId) {
        SimManager.find(sims, simId) ?: SimManager.find(sims, AppSettings.defaultSim) ?: sims.firstOrNull()
    }
    val caller = rememberCaller { if (sims.size > 1) activeSim?.handle else null }
    LaunchedEffect(Unit) { KeyFeedback.start(context) }
    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrEmpty()) raw = PhoneFormat.digits(initialNumber)
    }
    val suggestion by produceState<Suggestion?>(null, raw, data.contacts) {
        if (raw.isEmpty()) {
            value = null
        } else {
            delay(45)
            value = withContext(Dispatchers.Default) { DialSuggest.find(data.contacts, raw) }
        }
    }

    var codeText by remember { mutableStateOf<String?>(null) }
    fun runCode(code: String) {
        if (code == "*#06#") {
            val imei = DialCodes.imei(context)
            if (imei == null) DialCodes.openImeiScreen(context) else codeText = imei
            return
        }
        if (DialCodes.secretCode(context, code)) return
        caller(code)
    }

    fun press(digit: String) {
        KeyFeedback.press(digit)
        if (raw.length < 40) raw += digit
    }

    fun onCall() {
        KeyFeedback.tick()
        if (raw.isEmpty()) {
            val last = data.calls.firstOrNull { it.isOutgoing }
            if (last != null) raw = PhoneFormat.digits(last.number)
        } else {
            runCode(raw)
        }
    }

    codeText?.let { text ->
        Dialog(onDismissRequest = { codeText = null }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.card).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(text, style = IosType.body.copy(color = colors.label).ltr())
                Spacer(Modifier.height(16.dp))
                BasicText(
                    stringResource(R.string.done),
                    style = IosType.headline.copy(color = colors.tint),
                    modifier = Modifier.pressable(pressedScale = 0.95f) { codeText = null }.padding(8.dp),
                )
            }
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
                    GlassIconButton(
                        icon = Icons.Filled.PersonAdd,
                        onClick = { nav.push(Route.Editor(null, raw)) },
                        size = 48.dp,
                        iconSize = 22.dp,
                        tint = colors.tint,
                    )
                }
            }
            val shownNumber = remember(raw) { PhoneFormat.format(raw) }
            val numberSize = when {
                shownNumber.length <= 11 -> 44.sp
                shownNumber.length <= 14 -> 38.sp
                shownNumber.length <= 17 -> 32.sp
                else -> 26.sp
            }
            Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                BasicText(
                    text = shownNumber,
                    style = IosType.dialedNumber.copy(color = colors.label, textAlign = TextAlign.Center, fontSize = numberSize).ltr(),
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
                            val digits = PhoneFormat.digits(text).take(40)
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { key ->
                            val speed = key.digit.singleOrNull()?.takeIf { it in '2'..'9' }?.let { AppSettings.speedDial[it - '0'] }
                            PadKey(
                                digit = key.digit,
                                letters = key.letters,
                                size = keySize,
                                fill = colors.keyFill,
                                pressedFill = colors.keyPressed,
                                content = colors.label,
                                onPress = { press(key.digit) },
                                onLong = when {
                                    key.digit == "0" -> {
                                        {
                                            KeyFeedback.tick()
                                            raw = raw.dropLast(1) + "+"
                                        }
                                    }
                                    key.digit == "1" -> {
                                        {
                                            KeyFeedback.tick()
                                            raw = raw.dropLast(1)
                                            caller(VOICEMAIL)
                                        }
                                    }
                                    speed != null -> {
                                        {
                                            KeyFeedback.tick()
                                            raw = raw.dropLast(1)
                                            caller(speed.number)
                                        }
                                    }
                                    else -> null
                                },
                            )
                        }
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
                Box(Modifier.size(keySize), contentAlignment = Alignment.Center) {
                    if (sims.size > 1 && activeSim != null) {
                        SimChip(activeSim.slot + 1) { simMenu = true }
                        GlassMenu(
                            expanded = simMenu,
                            onDismiss = { simMenu = false },
                            alignment = Alignment.TopStart,
                            offsetY = (-(sims.size * 46 + 28)).dp,
                            width = 230.dp,
                            items = sims.map { sim ->
                                MenuItem(sim.label + "  ·  " + stringResource(R.string.sim_n, sim.slot + 1), checked = sim.id == activeSim.id) { simId = sim.id }
                            },
                        )
                    }
                }
                Spacer(Modifier.size(gap))
                CallButton(keySize, onTap = ::onCall)
                Spacer(Modifier.size(gap))
                DeleteSlot(
                    visible = raw.isNotEmpty(),
                    size = keySize,
                    onDelete = { step ->
                        if (step == 0 || step % 4 == 0) KeyFeedback.tick()
                        raw = raw.dropLast(1)
                    },
                    scope = scope,
                )
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun SimChip(number: Int, onClick: () -> Unit) {
    val colors = Ios.colors
    Column(
        Modifier.pressable(pressedScale = 0.9f, onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(Icons.Filled.SimCard, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.tint))
        BasicText(stringResource(R.string.sim_n, number), style = IosType.caption.copy(color = colors.tint, fontWeight = FontWeight.SemiBold))
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
            imageVector = IosIcons.Phone,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun DeleteButton(size: Dp, onDelete: (Int) -> Unit, scope: CoroutineScope) {
    val colors = Ios.colors
    val del = rememberUpdatedState(onDelete)
    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    del.value(0)
                    val job = scope.launch {
                        delay(380)
                        var step = 1
                        while (isActive) {
                            del.value(step)
                            delay(if (step < 8) 80L else 45L)
                            step++
                        }
                    }
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        job.cancel()
                    }
                }
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
private fun DeleteSlot(visible: Boolean, size: Dp, onDelete: (Int) -> Unit, scope: CoroutineScope) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
        ) {
            DeleteButton(size = size, onDelete = onDelete, scope = scope)
        }
    }
}
