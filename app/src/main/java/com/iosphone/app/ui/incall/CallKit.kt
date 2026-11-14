package com.iosphone.app.ui.incall

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import com.iosphone.app.R
import com.iosphone.app.data.BG_COLORS
import com.iosphone.app.data.BG_PHOTO
import com.iosphone.app.data.CallLook
import com.iosphone.app.ui.common.backdropColors

@Immutable
class CallPalette(
    val fg: Color,
    val fgSoft: Color,
    val ctrl: Color,
    val ctrlOn: Color,
    val ctrlOnIcon: Color,
    val pad: Color,
    val padPressed: Color,
    val accent: Color,
    val accept: Color,
    val decline: Color,
    val style: Int,
    val shape: Shape,
)

private fun lightBackdrop(look: CallLook): Boolean {
    if (look.bg == BG_PHOTO || look.contactPhoto) return false
    val stops = backdropColors(look).stops
    val lum = stops.map { it.luminance() }.average().toFloat() * (1f - look.dim)
    return lum > 0.4f
}

fun callPalette(look: CallLook): CallPalette {
    val darkText = when (look.textMode) {
        1 -> false
        2 -> true
        else -> lightBackdrop(look)
    }
    val fg = if (darkText) Color(0xFF0B0B0F) else Color.White
    val accent = Color(look.accent)
    val ctrl = when (look.buttonStyle) {
        1 -> accent.copy(alpha = 0.3f)
        2 -> Color.Transparent
        else -> fg.copy(alpha = 0.18f)
    }
    val ctrlOn = if (look.buttonStyle == 1) accent else fg
    val onIcon = when {
        look.buttonStyle == 1 -> if (accent.luminance() > 0.5f) Color(0xFF1C1C1E) else Color.White
        darkText -> Color.White
        else -> Color(0xFF1C1C1E)
    }
    return CallPalette(
        fg = fg,
        fgSoft = fg.copy(alpha = 0.68f),
        ctrl = ctrl,
        ctrlOn = ctrlOn,
        ctrlOnIcon = onIcon,
        pad = fg.copy(alpha = 0.16f),
        padPressed = fg.copy(alpha = 0.4f),
        accent = accent,
        accept = Color(look.accept),
        decline = Color(look.decline),
        style = look.buttonStyle,
        shape = if (look.buttonShape == 1) RoundedCornerShape(percent = 30) else CircleShape,
    )
}

fun avatarShapeOf(index: Int): Shape = when (index) {
    1 -> RoundedCornerShape(percent = 32)
    2 -> RoundedCornerShape(percent = 16)
    else -> CircleShape
}

class LookPreset(val nameRes: Int, val look: CallLook)

val LookPresets = listOf(
    LookPreset(R.string.look_classic, CallLook()),
    LookPreset(
        R.string.look_neon,
        CallLook(
            bg = BG_COLORS, c1 = 0xFF0B0F2B.toInt(), c2 = 0xFF7B2FFF.toInt(), c3 = 0xFF00A3C4.toInt(),
            angle = 160f, dim = 0.1f, glow = 1f, vignette = 0.45f, grain = 0.12f, animated = true,
            accent = 0xFF00F0FF.toInt(), accept = 0xFF00E676.toInt(), decline = 0xFFFF1744.toInt(),
            buttonStyle = 2, ring = 1, avatarShape = 1,
        ),
    ),
    LookPreset(
        R.string.look_glass,
        CallLook(bg = 1, blur = 0.7f, dim = 0.12f, glow = 0.9f, vignette = 0.2f, grain = 0.06f, avatarShape = 1, buttonShape = 1),
    ),
    LookPreset(
        R.string.look_daylight,
        CallLook(
            bg = BG_COLORS, c1 = 0xFFF5F7FF.toInt(), c2 = 0xFFDDE6FF.toInt(), c3 = 0xFFBFD0FF.toInt(),
            angle = 200f, dim = 0f, glow = 0.5f, vignette = 0.08f, grain = 0.04f, textMode = 2,
            accent = 0xFF3A6BFF.toInt(), accept = 0xFF2DBE60.toInt(), decline = 0xFFE5484D.toInt(), buttonStyle = 1,
        ),
    ),
    LookPreset(
        R.string.look_sunset,
        CallLook(bg = 2, dim = 0.12f, glow = 1f, animated = true, avatarShape = 1, ring = 0, grain = 0.08f),
    ),
    LookPreset(
        R.string.look_mono,
        CallLook(bg = 13, dim = 0.05f, glow = 0.6f, vignette = 0.4f, grain = 0.14f, buttonStyle = 2, accept = 0xFF30D158.toInt()),
    ),
    LookPreset(
        R.string.look_royal,
        CallLook(
            bg = BG_COLORS, c1 = 0xFF1A0B3B.toInt(), c2 = 0xFF4B1D8F.toInt(), c3 = 0xFF0B0620.toInt(),
            angle = 170f, glow = 0.9f, vignette = 0.4f, animated = true,
            accent = 0xFFFFD166.toInt(), buttonStyle = 1, avatarShape = 2, ring = 1,
        ),
    ),
    LookPreset(
        R.string.look_emerald,
        CallLook(bg = 5, dim = 0.1f, glow = 0.9f, animated = true, avatarShape = 2, ring = 1, accent = 0xFF52B788.toInt()),
    ),
)
