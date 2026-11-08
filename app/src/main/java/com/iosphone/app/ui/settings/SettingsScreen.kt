package com.iosphone.app.ui.settings

import androidx.compose.runtime.mutableStateOf
import com.iosphone.app.util.LocaleHelper
import android.app.Activity
import android.content.Context
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.iosRow
import com.iosphone.app.telecom.SimManager
import com.iosphone.app.data.LocalAppData
import androidx.compose.material.icons.filled.Check
import android.provider.Settings
import android.os.PowerManager
import android.os.Build
import android.content.Intent
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Photo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.incall.CallPreview
import com.iosphone.app.util.KeyFeedback
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
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
                stringResource(R.string.settings_title),
                style = IosType.largeTitle.copy(color = colors.label),
                modifier = Modifier.padding(start = 4.dp, bottom = 14.dp),
            )

            SectionHeader(stringResource(R.string.appearance))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.appearance_system),
                        stringResource(R.string.appearance_light),
                        stringResource(R.string.appearance_dark),
                    ),
                    selected = AppSettings.appearance,
                    onSelect = { AppSettings.setAppearance(it) },
                )
            }

            val langContext = LocalContext.current
            var lang by remember { mutableStateOf(LocaleHelper.get(langContext)) }
            val langCodes = listOf("system", "en", "ar")
            SectionHeader(stringResource(R.string.language))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.appearance_system),
                        "English",
                        "العربية",
                    ),
                    selected = langCodes.indexOf(lang).coerceAtLeast(0),
                    onSelect = { i ->
                        if (langCodes[i] != lang) {
                            lang = langCodes[i]
                            LocaleHelper.set(langContext, lang)
                            (langContext as? Activity)?.recreate()
                        }
                    },
                )
            }

            SectionHeader(stringResource(R.string.call_appearance))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewTile(stringResource(R.string.bg_incoming), AppSettings.incoming, true, Modifier.weight(1f)) {
                    nav.push(Route.CallStyle(true))
                }
                PreviewTile(stringResource(R.string.bg_active), AppSettings.active, false, Modifier.weight(1f)) {
                    nav.push(Route.CallStyle(false))
                }
            }

            SectionHeader(stringResource(R.string.call_settings))
            Card {
                NavRow(stringResource(R.string.quick_decline)) { nav.push(Route.QuickReplies) }
                Hairline(0.dp)
                NavRow(stringResource(R.string.speed_dial)) { nav.push(Route.SpeedDial) }
                Hairline(0.dp)
                NavRow(stringResource(R.string.blocked_numbers)) { nav.push(Route.Blocked) }
                Hairline(0.dp)
                NavRow(stringResource(R.string.answer_end)) { nav.push(Route.AnswerEnd) }
                Hairline(0.dp)
                NavRow(stringResource(R.string.supplementary)) { nav.push(Route.Supplementary) }
                Hairline(0.dp)
                NavRow(stringResource(R.string.other_call_settings)) { nav.push(Route.OtherCalls) }
            }

            SectionHeader(stringResource(R.string.keypad_section))
            Card {
                ToggleRow(stringResource(R.string.key_sounds), AppSettings.keySounds) { AppSettings.setKeySounds(it) }
                Hairline(0.dp)
                ToggleRow(stringResource(R.string.key_haptics), AppSettings.keyHaptics) { AppSettings.setKeyHaptics(it) }
                if (AppSettings.keyHaptics) {
                    Spacer(Modifier.height(8.dp))
                    Segmented(
                        options = listOf(
                            stringResource(R.string.haptic_light),
                            stringResource(R.string.haptic_medium),
                            stringResource(R.string.haptic_strong),
                        ),
                        selected = AppSettings.hapticLevel - 1,
                        onSelect = {
                            AppSettings.changeHapticLevel(it + 1)
                            KeyFeedback.tick()
                        },
                    )
                }
                if (AppSettings.keySounds) {
                    SliderRow(stringResource(R.string.key_volume), AppSettings.keyVolume / 100f) {
                        AppSettings.changeKeyVolume((it * 100).roundToInt())
                    }
                }
            }

            val appData = LocalAppData.current
            val ctx = LocalContext.current
            val sims = remember(appData.version) { SimManager.list(ctx) }
            if (sims.size > 1) {
                SectionHeader(stringResource(R.string.sim_section))
                Card {
                    OptionRow(stringResource(R.string.ask_every_time), null, AppSettings.defaultSim.isEmpty()) {
                        AppSettings.changeDefaultSim("")
                    }
                    sims.forEach { sim ->
                        Hairline(0.dp)
                        OptionRow(sim.label, stringResource(R.string.sim_n, sim.slot + 1), AppSettings.defaultSim == sim.id) {
                            AppSettings.changeDefaultSim(sim.id)
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.lock_section))
            val pkg = ctx.packageName
            val fullScreenOk = remember(appData.resumeCount) {
                if (Build.VERSION.SDK_INT >= 34) ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() else true
            }
            val batteryOk = remember(appData.resumeCount) {
                (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(pkg)
            }
            val notifOk = remember(appData.resumeCount) { ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled() }
            Card {
                StatusRow(stringResource(R.string.fullscreen_intent), fullScreenOk) {
                    val intent = if (Build.VERSION.SDK_INT >= 34) {
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg"))
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    }
                    runCatching { ctx.startActivity(intent) }
                }
                Hairline(0.dp)
                StatusRow(stringResource(R.string.battery_opt), batteryOk) {
                    val ok = runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg")))
                    }.isSuccess
                    if (!ok) runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }
                Hairline(0.dp)
                StatusRow(stringResource(R.string.notifications_title), notifOk) {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
                        )
                    }
                }
            }
            BasicText(
                stringResource(R.string.lock_footer),
                style = IosType.footnote.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )

            SectionHeader(stringResource(R.string.recording_section))
            Card {
                ToggleRow(stringResource(R.string.auto_record), AppSettings.autoRecord) { AppSettings.changeAutoRecord(it) }
                val a11yOk = remember(appData.resumeCount) { com.iosphone.app.telecom.RecordingAccessibility.enabled(ctx) }
                StatusRow(stringResource(R.string.rec_a11y), a11yOk) {
                    ctx.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                NavRow(stringResource(R.string.share_rec_log)) {
                    val text = runCatching {
                        java.io.File(ctx.filesDir, "recording.log").readText().takeLast(12000)
                    }.getOrDefault("").ifBlank { ctx.getString(R.string.rec_log_empty) }
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, text)
                    ctx.startActivity(android.content.Intent.createChooser(send, null))
                }
            }
            BasicText(
                stringResource(R.string.auto_record_footer),
                style = IosType.footnote.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )

            BasicText(
                stringResource(R.string.settings_footer),
                style = IosType.footnote.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 40.dp),
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String) {
    BasicText(
        text.uppercase(),
        style = IosType.footnote.copy(color = Ios.colors.secondaryLabel),
        modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
internal fun Card(content: @Composable ColumnScope.() -> Unit) {
    val colors = Ios.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.card).padding(16.dp),
        content = content,
    )
}

@Composable
internal fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = Ios.colors
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = IosType.body.copy(color = colors.label), modifier = Modifier.width(92.dp))
        IosSlider(value, onChange, Modifier.weight(1f))
    }
}

@Composable
internal fun OptionRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.fillMaxWidth().height(56.dp).iosRow(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = IosType.body.copy(color = colors.label), maxLines = 1)
            if (subtitle != null) BasicText(subtitle, style = IosType.footnote.copy(color = colors.secondaryLabel))
        }
        if (selected) Image(Icons.Filled.Check, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.tint))
    }
}

@Composable
internal fun StatusRow(title: String, ok: Boolean, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).iosRow(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = IosType.body.copy(color = colors.label), modifier = Modifier.weight(1f))
        BasicText(
            stringResource(if (ok) R.string.perm_allowed else R.string.perm_not_allowed),
            style = IosType.body.copy(color = if (ok) colors.secondaryLabel else colors.red),
        )
    }
}

@Composable
internal fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Ios.colors
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = IosType.body.copy(color = colors.label), modifier = Modifier.weight(1f))
        IosSwitch(checked, onChange)
    }
}

@Composable
internal fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Ios.colors
    val x by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "switchThumb",
    )
    val bg by animateColorAsState(
        targetValue = if (checked) colors.green else colors.fill,
        animationSpec = tween(220),
        label = "switchBg",
    )
    Box(
        Modifier
            .width(51.dp)
            .height(31.dp)
            .pressable(pressedScale = 0.97f) { onChange(!checked) }
            .clip(CircleShape)
            .background(bg)
            .padding(2.dp),
    ) {
        Box(Modifier.offset(x = x).size(27.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
    }
}

@Composable
internal fun IosSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ios.colors
    val density = LocalDensity.current
    val latest = rememberUpdatedState(onChange)
    var width by remember { mutableIntStateOf(1) }
    val thumb = 28.dp
    val thumbPx = with(density) { thumb.toPx() }
    Box(
        modifier
            .height(32.dp)
            .onSizeChanged { width = max(1, it.width) }
            .pointerInput(Unit) {
                detectTapGestures { o -> latest.value((o.x / width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        latest.value((change.position.x / width).coerceIn(0f, 1f))
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.fill))
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(colors.tint))
        Box(
            Modifier
                .offset { IntOffset(((width - thumbPx) * value.coerceIn(0f, 1f)).roundToInt(), 0) }
                .size(thumb)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
internal fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = Ios.colors
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(colors.fill),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(
            targetValue = w * selected,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (colors.dark) Color(0xFF636366) else Color.White),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().pressable(pressedScale = 0.97f) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        label,
                        style = IosType.callout.copy(
                            color = colors.label,
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

internal suspend fun importImage(context: Context, uri: Uri, out: File): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2200) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (decoded == null) {
            false
        } else {
            val orientation = runCatching {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val upright = if (degrees == 0f) {
                decoded
            } else {
                val m = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            }
            out.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }
    }.getOrDefault(false)
}


@Composable
internal fun NavRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).iosRow(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = IosType.body.copy(color = colors.label), maxLines = 1)
            if (subtitle != null) BasicText(subtitle, style = IosType.footnote.copy(color = colors.secondaryLabel), maxLines = 1)
        }
        Image(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(colors.secondaryLabel),
        )
    }
}

@Composable
private fun PreviewTile(title: String, cfg: com.iosphone.app.data.CallBgSettings, incoming: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = Ios.colors
    Column(
        modifier.pressable(pressedScale = 0.97f, onClick = onClick).clip(RoundedCornerShape(20.dp)).background(colors.card).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallPreview(cfg, incoming, Modifier.fillMaxWidth(0.62f))
        Spacer(Modifier.height(10.dp))
        BasicText(title, style = IosType.callout.copy(color = colors.label, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}
