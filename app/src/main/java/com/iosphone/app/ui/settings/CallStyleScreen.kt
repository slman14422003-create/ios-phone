package com.iosphone.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Photo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iosphone.app.R
import com.iosphone.app.data.AppSettings
import com.iosphone.app.data.BG_COLORS
import com.iosphone.app.data.BG_PHOTO
import com.iosphone.app.data.CallLook
import com.iosphone.app.ui.common.BackdropPresets
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.backdropColors
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.incall.CallPreview
import com.iosphone.app.ui.incall.CallPreviewFull
import com.iosphone.app.ui.incall.LookPresets
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import kotlinx.coroutines.launch

@Composable
fun CallStyleScreen(incoming: Boolean) {
    val colors = Ios.colors
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg = if (incoming) AppSettings.incoming else AppSettings.active
    val other = if (incoming) AppSettings.active else AppSettings.incoming
    val look = cfg.look
    var full by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { cfg.flush() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (importImage(context, uri, cfg.file(context))) cfg.photoImported()
            }
        }
    }
    fun pick() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val pageBg = if (colors.dark) colors.background else colors.grouped
    Column(Modifier.fillMaxSize().background(pageBg)) {
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
            Spacer(Modifier.width(12.dp))
            BasicText(
                stringResource(if (incoming) R.string.bg_incoming else R.string.bg_active),
                style = IosType.title3.copy(color = colors.label),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(290.dp)
                .pressable(pressedScale = 0.98f) { full = true }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            CallPreview(cfg, incoming, Modifier.fillMaxHeight())
        }
        BasicText(
            stringResource(R.string.tap_preview),
            style = IosType.footnote.copy(color = colors.secondaryLabel),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            SectionHeader(stringResource(R.string.looks))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LookPresets.forEach { preset ->
                    val c = backdropColors(preset.look)
                    Box(
                        Modifier
                            .size(width = 104.dp, height = 70.dp)
                            .pressable(pressedScale = 0.95f) {
                                cfg.update { preset.look.copy(photoVersion = it.photoVersion) }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(c.stops + c.blobs.take(1)))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        BasicText(
                            stringResource(preset.nameRes),
                            style = IosType.footnote.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(10.dp),
                            maxLines = 1,
                        )
                    }
                }
            }

            SectionHeader(stringResource(R.string.background))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.bg_gradient),
                        stringResource(R.string.bg_colors),
                        stringResource(R.string.bg_photo),
                    ),
                    selected = when {
                        look.bg >= 0 -> 0
                        look.bg == BG_COLORS -> 1
                        else -> 2
                    },
                    onSelect = { i ->
                        when (i) {
                            0 -> if (look.bg < 0) cfg.update { it.copy(bg = 0) }
                            1 -> cfg.update { it.copy(bg = BG_COLORS) }
                            else -> if (cfg.hasCustom(context)) cfg.update { it.copy(bg = BG_PHOTO) } else pick()
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))
                when {
                    look.bg >= 0 -> Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BackdropPresets.forEachIndexed { i, p ->
                            Swatch(
                                selected = look.bg == i,
                                brush = Brush.linearGradient(listOf(Color(p.blobs[0]), Color(p.top), Color(p.bottom))),
                                onClick = { cfg.update { it.copy(bg = i) } },
                            )
                        }
                    }
                    look.bg == BG_COLORS -> Column {
                        ColorField(stringResource(R.string.color_top), look.c1) { c -> cfg.update { it.copy(c1 = c) } }
                        Hairline(0.dp)
                        ColorField(stringResource(R.string.color_middle), look.c2) { c -> cfg.update { it.copy(c2 = c) } }
                        Hairline(0.dp)
                        ColorField(stringResource(R.string.color_bottom), look.c3) { c -> cfg.update { it.copy(c3 = c) } }
                        SliderRow(stringResource(R.string.angle), look.angle / 360f) { v -> cfg.update { it.copy(angle = v * 360f) } }
                    }
                    else -> Column {
                        Row(
                            Modifier.fillMaxWidth().pressable(pressedScale = 0.98f) { pick() }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(Icons.Filled.Photo, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.tint))
                            Spacer(Modifier.width(10.dp))
                            BasicText(stringResource(R.string.choose_photo), style = IosType.body.copy(color = colors.tint))
                        }
                    }
                }
                Hairline(0.dp)
                ToggleRow(stringResource(R.string.use_contact_photo), look.contactPhoto) { v -> cfg.update { it.copy(contactPhoto = v) } }
            }

            SectionHeader(stringResource(R.string.effects))
            Card {
                SliderRow(stringResource(R.string.blur), look.blur) { v -> cfg.update { it.copy(blur = v) } }
                SliderRow(stringResource(R.string.dim), look.dim / 0.8f) { v -> cfg.update { it.copy(dim = v * 0.8f) } }
                SliderRow(stringResource(R.string.glow), look.glow) { v -> cfg.update { it.copy(glow = v) } }
                SliderRow(stringResource(R.string.vignette), look.vignette) { v -> cfg.update { it.copy(vignette = v) } }
                SliderRow(stringResource(R.string.grain), look.grain) { v -> cfg.update { it.copy(grain = v) } }
                Spacer(Modifier.height(6.dp))
                ToggleRow(stringResource(R.string.animated_bg), look.animated) { v -> cfg.update { it.copy(animated = v) } }
            }

            SectionHeader(stringResource(R.string.text_section))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.text_auto),
                        stringResource(R.string.text_light),
                        stringResource(R.string.text_dark),
                    ),
                    selected = look.textMode,
                    onSelect = { i -> cfg.update { it.copy(textMode = i) } },
                )
                SliderRow(stringResource(R.string.name_size), look.nameSize) { v -> cfg.update { it.copy(nameSize = v) } }
                Spacer(Modifier.height(6.dp))
                ToggleRow(stringResource(R.string.show_number), look.showNumber) { v -> cfg.update { it.copy(showNumber = v) } }
                ToggleRow(stringResource(R.string.show_sim), look.showSim) { v -> cfg.update { it.copy(showSim = v) } }
            }

            SectionHeader(stringResource(R.string.buttons))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.style_glass),
                        stringResource(R.string.style_solid),
                        stringResource(R.string.style_outline),
                    ),
                    selected = look.buttonStyle,
                    onSelect = { i -> cfg.update { it.copy(buttonStyle = i) } },
                )
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(stringResource(R.string.shape_circle), stringResource(R.string.shape_rounded)),
                    selected = look.buttonShape,
                    onSelect = { i -> cfg.update { it.copy(buttonShape = i) } },
                )
                Spacer(Modifier.height(6.dp))
                ColorField(stringResource(R.string.color_accept), look.accept) { c -> cfg.update { it.copy(accept = c) } }
                Hairline(0.dp)
                ColorField(stringResource(R.string.color_decline), look.decline) { c -> cfg.update { it.copy(decline = c) } }
                Hairline(0.dp)
                ColorField(stringResource(R.string.color_accent), look.accent) { c -> cfg.update { it.copy(accent = c) } }
            }

            SectionHeader(stringResource(R.string.avatar))
            Card {
                Segmented(
                    options = listOf(
                        stringResource(R.string.shape_circle),
                        stringResource(R.string.shape_squircle),
                        stringResource(R.string.shape_square),
                    ),
                    selected = look.avatarShape,
                    onSelect = { i -> cfg.update { it.copy(avatarShape = i) } },
                )
                SliderRow(stringResource(R.string.avatar_size), look.avatarSize) { v -> cfg.update { it.copy(avatarSize = v) } }
                Spacer(Modifier.height(10.dp))
                if (incoming) {
                    Segmented(
                        options = listOf(
                            stringResource(R.string.ring_pulse),
                            stringResource(R.string.ring_glow),
                            stringResource(R.string.ring_none),
                        ),
                        selected = look.ring,
                        onSelect = { i -> cfg.update { it.copy(ring = i) } },
                    )
                } else {
                    ToggleRow(stringResource(R.string.avatar_in_call), look.avatarActive) { v -> cfg.update { it.copy(avatarActive = v) } }
                }
            }

            SectionHeader(stringResource(R.string.actions))
            Card {
                NavRow(stringResource(if (incoming) R.string.copy_to_active else R.string.copy_to_incoming)) {
                    val copy = look.copy(bg = if (look.bg == BG_PHOTO && !other.hasCustom(context)) 0 else look.bg)
                    other.update { copy.copy(photoVersion = it.photoVersion) }
                    other.flush()
                }
                Hairline(0.dp)
                Row(
                    Modifier.fillMaxWidth().height(52.dp).pressable(pressedScale = 0.99f) {
                        cfg.update { CallLook(photoVersion = it.photoVersion) }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(stringResource(R.string.reset_look), style = IosType.body.copy(color = colors.red))
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (full) {
        Dialog(
            onDismissRequest = { full = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { full = false } }) {
                CallPreviewFull(cfg, incoming)
            }
        }
    }
}

@Composable
private fun Swatch(selected: Boolean, brush: Brush, onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        Modifier
            .size(54.dp)
            .pressable(pressedScale = 0.92f, onClick = onClick)
            .then(if (selected) Modifier.border(3.dp, colors.tint, CircleShape) else Modifier)
            .padding(5.dp)
            .clip(CircleShape)
            .background(brush),
    )
}
