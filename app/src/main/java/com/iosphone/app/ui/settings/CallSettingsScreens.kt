package com.iosphone.app.ui.settings

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.data.ContactsWriter
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.data.SpeedEntry
import com.iosphone.app.telecom.QuickReply
import com.iosphone.app.telecom.SimManager
import com.iosphone.app.telecom.Ussd
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.ui.theme.ltr
import com.iosphone.app.util.PhoneFormat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun SettingsPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = Ios.colors
    val nav = LocalNav.current
    val pageBg = if (colors.dark) colors.background else colors.grouped
    Column(Modifier.fillMaxSize().background(pageBg)) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            BasicText(
                title,
                style = IosType.largeTitle.copy(color = colors.label),
                modifier = Modifier.padding(start = 4.dp, bottom = 14.dp),
            )
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Footer(text: String) {
    BasicText(
        text,
        style = IosType.footnote.copy(color = Ios.colors.secondaryLabel),
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
    )
}

@Composable
private fun TextAction(title: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.fillMaxWidth().height(50.dp).pressable(pressedScale = 0.99f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = IosType.body.copy(color = if (destructive) colors.red else colors.tint))
    }
}

@Composable
private fun Pill(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        modifier
            .height(40.dp)
            .pressable(pressedScale = 0.96f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(title, style = IosType.callout.copy(color = colors.tint, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, phone: Boolean = false) {
    val colors = Ios.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.fill)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) BasicText(hint, style = IosType.body.copy(color = colors.secondaryLabel), maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = (if (phone) IosType.body.ltr() else IosType.body).copy(color = colors.label),
            cursorBrush = SolidColor(colors.tint),
            keyboardOptions = KeyboardOptions(keyboardType = if (phone) KeyboardType.Phone else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun QuickRepliesScreen() {
    val colors = Ios.colors
    val context = LocalContext.current
    val defaults = listOf(
        stringResource(R.string.qr_1),
        stringResource(R.string.qr_2),
        stringResource(R.string.qr_3),
        stringResource(R.string.qr_4),
    )
    val items = remember { mutableStateListOf<String>().apply { addAll(AppSettings.quickReplies ?: defaults) } }
    var smsOk by remember { mutableStateOf(QuickReply.canSend(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { smsOk = it }
    SettingsPage(stringResource(R.string.quick_decline)) {
        Card {
            items.forEachIndexed { i, text ->
                if (i > 0) Hairline(0.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = text,
                        onValueChange = {
                            items[i] = it.take(160)
                            AppSettings.changeQuickReplies(items.toList())
                        },
                        textStyle = IosType.body.copy(color = colors.label),
                        cursorBrush = SolidColor(colors.tint),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.size(36.dp).pressable(pressedScale = 0.9f) {
                            items.removeAt(i)
                            AppSettings.changeQuickReplies(items.toList())
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(Icons.Filled.Close, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
                    }
                }
            }
            if (items.size < 8) {
                if (items.isNotEmpty()) Hairline(0.dp)
                TextAction(stringResource(R.string.add_message)) {
                    items.add(stringResource0(context, R.string.qr_new))
                    AppSettings.changeQuickReplies(items.toList())
                }
            }
            Hairline(0.dp)
            TextAction(stringResource(R.string.reset_defaults), destructive = true) {
                items.clear()
                items.addAll(defaults)
                AppSettings.changeQuickReplies(null)
            }
        }
        SectionHeader(stringResource(R.string.sms_permission))
        Card {
            StatusRow(stringResource(R.string.send_sms_directly), smsOk) {
                if (!smsOk) launcher.launch(Manifest.permission.SEND_SMS)
            }
        }
        Footer(stringResource(R.string.sms_footer))
    }
}

private fun stringResource0(context: Context, id: Int): String = context.getString(id)

@Composable
fun SpeedDialScreen() {
    val colors = Ios.colors
    val data = LocalAppData.current
    var slot by remember { mutableStateOf(0) }
    SettingsPage(stringResource(R.string.speed_dial)) {
        Card {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText("1", style = IosType.title3.copy(color = colors.tint), modifier = Modifier.width(36.dp))
                BasicText(stringResource(R.string.voicemail_title), style = IosType.body.copy(color = colors.label))
            }
            for (n in 2..9) {
                Hairline(0.dp)
                val entry = AppSettings.speedDial[n]
                Row(
                    Modifier.fillMaxWidth().height(56.dp).pressable(pressedScale = 0.99f) { slot = n },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(n.toString(), style = IosType.title3.copy(color = colors.tint), modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            if (entry == null) stringResource(R.string.not_assigned) else entry.name.ifBlank { PhoneFormat.format(entry.number) },
                            style = IosType.body.copy(color = if (entry == null) colors.secondaryLabel else colors.label),
                            maxLines = 1,
                        )
                        if (entry != null && entry.name.isNotBlank()) {
                            BasicText(PhoneFormat.format(entry.number), style = IosType.footnote.copy(color = colors.secondaryLabel).ltr(), maxLines = 1)
                        }
                    }
                    if (entry != null) {
                        Box(
                            Modifier.size(36.dp).pressable(pressedScale = 0.9f) { AppSettings.changeSpeedDial(n, null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(Icons.Filled.Close, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
                        }
                    }
                }
            }
        }
        Footer(stringResource(R.string.speed_dial_footer))
    }
    if (slot != 0) {
        var query by remember { mutableStateOf("") }
        val rows = remember(data.contacts, query) {
            val q = query.trim().lowercase()
            data.contacts
                .flatMap { c -> c.phones.map { c to it } }
                .filter { (c, p) -> q.isEmpty() || c.name.lowercase().contains(q) || PhoneFormat.digits(p.number).contains(PhoneFormat.digits(q).ifEmpty { "\u0000" }) }
        }
        Dialog(onDismissRequest = { slot = 0 }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.card)
                    .padding(16.dp),
            ) {
                Field(query, { query = it }, stringResource(R.string.search))
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(rows) { (c, p) ->
                        Column(
                            Modifier.fillMaxWidth().pressable(pressedScale = 0.99f) {
                                AppSettings.changeSpeedDial(slot, SpeedEntry(c.name, p.number))
                                slot = 0
                            }.padding(vertical = 10.dp),
                        ) {
                            BasicText(c.name, style = IosType.body.copy(color = colors.label), maxLines = 1)
                            BasicText(PhoneFormat.format(p.number), style = IosType.footnote.copy(color = colors.secondaryLabel).ltr(), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private class Blocked(val id: Long, val number: String)

private suspend fun loadBlocked(context: Context): List<Blocked> = withContext(Dispatchers.IO) {
    runCatching {
        if (!BlockedNumberContract.canCurrentUserBlockNumbers(context)) return@runCatching emptyList<Blocked>()
        val out = ArrayList<Blocked>()
        context.contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID, BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) out.add(Blocked(c.getLong(0), c.getString(1).orEmpty()))
        }
        out
    }.getOrDefault(emptyList())
}

@Composable
fun BlockedScreen() {
    val colors = Ios.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Blocked>>(emptyList()) }
    var version by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    val can = remember { runCatching { BlockedNumberContract.canCurrentUserBlockNumbers(context) }.getOrDefault(false) }
    LaunchedEffect(version) { list = loadBlocked(context) }
    SettingsPage(stringResource(R.string.blocked_numbers)) {
        if (!can) {
            Card { BasicText(stringResource(R.string.blocked_fail), style = IosType.body.copy(color = colors.label)) }
        } else {
            Card {
                Field(input, { input = it }, stringResource(R.string.number_hint), phone = true)
                Spacer(Modifier.height(10.dp))
                Pill(stringResource(R.string.block_number), Modifier.fillMaxWidth()) {
                    val n = input.trim()
                    if (n.isNotEmpty()) {
                        scope.launch {
                            ContactsWriter.block(context, n)
                            input = ""
                            version++
                        }
                    }
                }
            }
            SectionHeader(stringResource(R.string.blocked_list))
            Card {
                if (list.isEmpty()) {
                    BasicText(stringResource(R.string.no_blocked), style = IosType.body.copy(color = colors.secondaryLabel))
                }
                list.forEachIndexed { i, b ->
                    if (i > 0) Hairline(0.dp)
                    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            PhoneFormat.format(b.number).ifEmpty { b.number },
                            style = IosType.body.copy(color = colors.label).ltr(),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Pill(stringResource(R.string.unblock), Modifier.width(96.dp)) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { BlockedNumberContract.unblock(context, b.number) }
                                }
                                version++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerEndScreen() {
    val values = listOf(0, 3, 5, 10)
    val ctx = LocalContext.current
    SettingsPage(stringResource(R.string.answer_end)) {
        SectionHeader(stringResource(R.string.auto_answer))
        Card {
            Segmented(
                options = listOf(
                    stringResource(R.string.off),
                    stringResource(R.string.seconds_n, 3),
                    stringResource(R.string.seconds_n, 5),
                    stringResource(R.string.seconds_n, 10),
                ),
                selected = values.indexOf(AppSettings.autoAnswerSeconds).coerceAtLeast(0),
                onSelect = { AppSettings.changeAutoAnswer(values[it]) },
            )
        }
        Footer(stringResource(R.string.auto_answer_footer))
        SectionHeader(stringResource(R.string.call_feedback))
        Card {
            ToggleRow(stringResource(R.string.vibrate_answer), AppSettings.vibrateOnAnswer) { AppSettings.changeVibrateOnAnswer(it) }
            Hairline(0.dp)
            ToggleRow(stringResource(R.string.vibrate_end), AppSettings.vibrateOnEnd) { AppSettings.changeVibrateOnEnd(it) }
            Hairline(0.dp)
            ToggleRow(stringResource(R.string.flash_ring), AppSettings.flashOnRing) { AppSettings.changeFlashOnRing(it) }
        }
        SectionHeader(stringResource(R.string.incoming_display))
        Card {
            Segmented(
                options = listOf(stringResource(R.string.display_auto), stringResource(R.string.display_full)),
                selected = AppSettings.incomingDisplay,
                onSelect = { AppSettings.changeIncomingDisplay(it) },
            )
        }
        Footer(stringResource(R.string.display_footer))
        SectionHeader(stringResource(R.string.ringtone_section))
        Card {
            NavRow(stringResource(R.string.ringtone_vibration)) {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
}

private class UssdResult(val ok: Boolean, val text: String)

@Composable
fun SupplementaryScreen() {
    val colors = Ios.colors
    val context = LocalContext.current
    val sims = remember { SimManager.list(context) }
    var simIndex by remember { mutableStateOf(0) }
    var forwardTo by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UssdResult?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    val subId = sims.getOrNull(simIndex)?.subId ?: -1

    fun send(code: String) {
        busy = true
        Ussd.run(context, code, subId) { ok, text ->
            busy = false
            result = UssdResult(ok, if (ok) text else context.getString(R.string.ussd_failed) + "\n($text)")
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val code = pending
        pending = null
        if (granted && code != null) send(code)
    }

    fun request(code: String) {
        if (Ussd.allowed(context)) send(code) else {
            pending = code
            launcher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    SettingsPage(stringResource(R.string.supplementary)) {
        if (sims.size > 1) {
            SectionHeader(stringResource(R.string.sim_section))
            Card {
                Segmented(
                    options = sims.map { it.label },
                    selected = simIndex,
                    onSelect = { simIndex = it },
                )
            }
        }
        SectionHeader(stringResource(R.string.call_waiting))
        Card {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.ussd_on), Modifier.weight(1f)) { request("*43#") }
                Pill(stringResource(R.string.ussd_off), Modifier.weight(1f)) { request("#43#") }
                Pill(stringResource(R.string.ussd_check), Modifier.weight(1f)) { request("*#43#") }
            }
        }
        SectionHeader(stringResource(R.string.call_forwarding))
        Card {
            Field(forwardTo, { forwardTo = it.filter { c -> c.isDigit() || c == '+' }.take(20) }, stringResource(R.string.forward_to), phone = true)
            val kinds = listOf(
                R.string.fwd_all to "21",
                R.string.fwd_busy to "67",
                R.string.fwd_no_answer to "61",
                R.string.fwd_unreachable to "62",
            )
            kinds.forEach { (label, code) ->
                Spacer(Modifier.height(14.dp))
                BasicText(stringResource(label), style = IosType.callout.copy(color = colors.label, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(R.string.ussd_on), Modifier.weight(1f)) {
                        if (forwardTo.isNotEmpty()) request("**$code*$forwardTo#")
                    }
                    Pill(stringResource(R.string.ussd_off), Modifier.weight(1f)) { request("##$code#") }
                    Pill(stringResource(R.string.ussd_check), Modifier.weight(1f)) { request("*#$code#") }
                }
            }
        }
        SectionHeader(stringResource(R.string.caller_id))
        Card {
            Pill(stringResource(R.string.ussd_check), Modifier.fillMaxWidth()) { request("*#31#") }
        }
        Footer(stringResource(R.string.ussd_footer))
    }
    if (busy || result != null) {
        Dialog(onDismissRequest = { if (!busy) result = null }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.card).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val r = result
                BasicText(
                    if (busy || r == null) stringResource(R.string.ussd_running) else r.text,
                    style = IosType.body.copy(color = colors.label),
                )
                if (!busy) {
                    Spacer(Modifier.height(14.dp))
                    Pill(stringResource(R.string.done), Modifier.fillMaxWidth()) { result = null }
                }
            }
        }
    }
}

@Composable
fun OtherCallsScreen() {
    val colors = Ios.colors
    val context = LocalContext.current
    val data = LocalAppData.current
    val clipboard = LocalClipboardManager.current
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("")
    }
    val crash = remember { File(context.filesDir, "crash.log") }
    SettingsPage(stringResource(R.string.other_call_settings)) {
        Card {
            ToggleRow(stringResource(R.string.hide_no_number), AppSettings.hideNoNumber) {
                AppSettings.changeHideNoNumber(it)
                data.invalidate()
            }
            Hairline(0.dp)
            ToggleRow(stringResource(R.string.roaming_assist), AppSettings.roamingAssist) { AppSettings.changeRoamingAssist(it) }
        }
        Footer(stringResource(R.string.roaming_footer))
        SectionHeader(stringResource(R.string.voicemail_title))
        Card {
            NavRow(stringResource(R.string.voicemail_settings)) {
                val ok = runCatching {
                    context.startActivity(Intent(TelephonyManager.ACTION_CONFIGURE_VOICEMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
                if (!ok) runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        SectionHeader(stringResource(R.string.about_section))
        Card {
            NavRow(stringResource(R.string.app_permissions)) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            Hairline(0.dp)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(stringResource(R.string.version), style = IosType.body.copy(color = colors.label), modifier = Modifier.weight(1f))
                BasicText(version, style = IosType.body.copy(color = colors.secondaryLabel))
            }
            if (crash.exists() && crash.length() > 0) {
                Hairline(0.dp)
                TextAction(stringResource(R.string.copy_crash_log)) {
                    runCatching { clipboard.setText(AnnotatedString(crash.readText().takeLast(20000))) }
                }
                Hairline(0.dp)
                TextAction(stringResource(R.string.clear_crash_log), destructive = true) {
                    runCatching { crash.delete() }
                }
            }
        }
    }
}
