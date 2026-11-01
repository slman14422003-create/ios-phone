package com.iosphone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class IosColors(
    val background: Color,
    val secondaryBackground: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val fill: Color,
    val fillPressed: Color,
    val tint: Color,
    val green: Color,
    val red: Color,
    val bar: Color,
    val barSeparator: Color,
    val dark: Boolean,
)

val IosLight = IosColors(
    background = Color(0xFFFFFFFF),
    secondaryBackground = Color(0xFFF2F2F7),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    separator = Color(0x4A3C3C43),
    fill = Color(0xFFE5E5EA),
    fillPressed = Color(0xFFC7C7CC),
    tint = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    bar = Color(0xE6F9F9F9),
    barSeparator = Color(0x4D000000),
    dark = false,
)

val IosDark = IosColors(
    background = Color(0xFF000000),
    secondaryBackground = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    separator = Color(0x99545458),
    fill = Color(0xFF333333),
    fillPressed = Color(0xFF6E6E73),
    tint = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    bar = Color(0xE61D1D1D),
    barSeparator = Color(0x99545458),
    dark = true,
)

val LocalIosColors = staticCompositionLocalOf { IosLight }

object IosType {
    private val base = FontFamily.SansSerif
    val largeTitle = TextStyle(fontFamily = base, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp)
    val title3 = TextStyle(fontFamily = base, fontSize = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.38.sp)
    val headline = TextStyle(fontFamily = base, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.43).sp)
    val body = TextStyle(fontFamily = base, fontSize = 17.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.43).sp)
    val footnote = TextStyle(fontFamily = base, fontSize = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.08).sp)
    val tabLabel = TextStyle(fontFamily = base, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.sp)
    val keypadDigit = TextStyle(fontFamily = base, fontSize = 36.sp, fontWeight = FontWeight.Light)
    val keypadLetters = TextStyle(fontFamily = base, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    val dialedNumber = TextStyle(fontFamily = base, fontSize = 36.sp, fontWeight = FontWeight.Normal)
}

@Composable
fun IosTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) IosDark else IosLight
    CompositionLocalProvider(LocalIosColors provides colors, content = content)
}

object Ios {
    val colors: IosColors
        @Composable get() = LocalIosColors.current
}
