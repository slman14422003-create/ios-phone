package com.iosphone.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iosphone.app.data.AppSettings

class IosColors(
    val background: Color,
    val grouped: Color,
    val card: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val fill: Color,
    val searchFill: Color,
    val tint: Color,
    val green: Color,
    val red: Color,
    val glass: Color,
    val glassBorder: Color,
    val selection: Color,
    val keyFill: Color,
    val keyPressed: Color,
    val blurTint: Color,
    val avatarTop: Color,
    val avatarBottom: Color,
    val dark: Boolean,
)

val IosLight = IosColors(
    background = Color(0xFFFFFFFF),
    grouped = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    separator = Color(0x4A3C3C43),
    fill = Color(0x1F787880),
    searchFill = Color(0x14000000),
    tint = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    glass = Color(0xD1FFFFFF),
    glassBorder = Color(0x14000000),
    selection = Color(0x1F767680),
    keyFill = Color(0xFFE9E9EB),
    keyPressed = Color(0xFFB9B9BF),
    blurTint = Color(0xA6FFFFFF),
    avatarTop = Color(0xFFB7BFDB),
    avatarBottom = Color(0xFF8E98BD),
    dark = false,
)

val IosDark = IosColors(
    background = Color(0xFF000000),
    grouped = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    separator = Color(0x99545458),
    fill = Color(0x5C787880),
    searchFill = Color(0x2EFFFFFF),
    tint = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    glass = Color(0xB82C2C2E),
    glassBorder = Color(0x29FFFFFF),
    selection = Color(0x40FFFFFF),
    keyFill = Color(0xFF2A2A2C),
    keyPressed = Color(0xFF6B6B70),
    blurTint = Color(0x992A2A2C),
    avatarTop = Color(0xFF7B84A6),
    avatarBottom = Color(0xFF555D80),
    dark = true,
)

val LocalIosColors = staticCompositionLocalOf { IosLight }

object IosType {
    private val base = FontFamily.SansSerif
    val largeTitle = TextStyle(fontFamily = base, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp)
    val title1 = TextStyle(fontFamily = base, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    val title3 = TextStyle(fontFamily = base, fontSize = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.38.sp)
    val headline = TextStyle(fontFamily = base, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.43).sp)
    val body = TextStyle(fontFamily = base, fontSize = 17.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.43).sp)
    val callout = TextStyle(fontFamily = base, fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp)
    val footnote = TextStyle(fontFamily = base, fontSize = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.08).sp)
    val caption = TextStyle(fontFamily = base, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val tabLabel = TextStyle(fontFamily = base, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.sp)
    val keypadDigit = TextStyle(fontFamily = base, fontSize = 36.sp, fontWeight = FontWeight.Normal)
    val keypadLetters = TextStyle(fontFamily = base, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
    val dialedNumber = TextStyle(fontFamily = base, fontSize = 46.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun IosTheme(syncSystemBars: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (AppSettings.appearance) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) IosDark else IosLight
    if (syncSystemBars) {
        // Keep status/navigation bar icons readable when the user forces light/dark in Settings.
        val activity = LocalContext.current as? ComponentActivity
        SideEffect {
            val bars = if (dark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            activity?.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        }
    }
    CompositionLocalProvider(LocalIosColors provides colors, content = content)
}

object Ios {
    val colors: IosColors
        @Composable get() = LocalIosColors.current
}
