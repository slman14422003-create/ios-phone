package com.iosphone.app.ui.keypad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Key(val digit: String, val letters: String)

private val keys = listOf(
    listOf(Key("1", ""), Key("2", "ABC"), Key("3", "DEF")),
    listOf(Key("4", "GHI"), Key("5", "JKL"), Key("6", "MNO")),
    listOf(Key("7", "PQRS"), Key("8", "TUV"), Key("9", "WXYZ")),
    listOf(Key("*", ""), Key("0", "+"), Key("#", "")),
)

@Composable
fun KeypadScreen(modifier: Modifier = Modifier) {
    val colors = Ios.colors
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf("") }

    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun placeCall() {
        if (raw.isEmpty()) return
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        tm.placeCall(Uri.fromParts("tel", raw, null), Bundle())
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) placeCall()
    }

    fun onCallTap() {
        tick()
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            placeCall()
        } else {
            permission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BasicText(
                text = PhoneFormat.format(raw),
                style = IosType.dialedNumber.copy(color = colors.label, textAlign = TextAlign.Center),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { key ->
                        KeypadKey(
                            key = key,
                            onTap = {
                                tick()
                                raw += key.digit
                            },
                            onLong = when (key.digit) {
                                "0" -> {
                                    {
                                        tick()
                                        raw += "+"
                                    }
                                }
                                else -> null
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.size(75.dp))
            Spacer(Modifier.size(20.dp))
            CallButton(onTap = ::onCallTap)
            Spacer(Modifier.size(20.dp))
            Box(modifier = Modifier.size(75.dp), contentAlignment = Alignment.Center) {
                if (raw.isNotEmpty()) {
                    DeleteButton(
                        onDelete = {
                            tick()
                            raw = raw.dropLast(1)
                        },
                        scopeJob = { block -> scope.launch { block() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(key: Key, onTap: () -> Unit, onLong: (() -> Unit)?) {
    val colors = Ios.colors
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(75.dp)
            .clip(CircleShape)
            .background(if (pressed) colors.fillPressed else colors.fill)
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
            if (key.letters.isNotEmpty() && key.digit != "0") {
                BasicText(key.letters, style = IosType.keypadLetters.copy(color = colors.label))
            } else if (key.digit == "0") {
                BasicText("+", style = IosType.keypadLetters.copy(color = colors.label))
            }
        }
    }
}

@Composable
private fun CallButton(onTap: () -> Unit) {
    val colors = Ios.colors
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(75.dp)
            .scale(if (pressed) 0.94f else 1f)
            .clip(CircleShape)
            .background(if (pressed) colors.green.copy(alpha = 0.75f) else colors.green)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit, scopeJob: (suspend () -> Unit) -> Job) {
    val colors = Ios.colors
    Box(
        modifier = Modifier
            .size(75.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDelete()
                        val job = scopeJob {
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
        Image(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.secondaryLabel),
            modifier = Modifier.size(28.dp),
        )
    }
}
