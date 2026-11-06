package com.iosphone.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.ui.theme.IosType

/**
 * One round dial-pad key, iOS style: the digit always sits on the same baseline (even for "1" and "*"),
 * the letters sit under it, and on press the fill lights up instantly then fades out slowly.
 */
@Composable
fun PadKey(
    digit: String,
    letters: String,
    size: Dp,
    fill: Color,
    pressedFill: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLong: (() -> Unit)? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        targetValue = if (pressed) pressedFill else fill,
        animationSpec = if (pressed) tween<Color>(30) else tween<Color>(420),
        label = "padKeyFill",
    )
    val k = size.value / 80f
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .pointerInput(onTap, onLong) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = onLong?.let { l -> { _: Offset -> l() } },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(44.dp * k), contentAlignment = Alignment.Center) {
                BasicText(
                    digit,
                    style = IosType.keypadDigit.copy(color = content, fontSize = (if (digit == "*") 44f * k else 36f * k).sp),
                    modifier = if (digit == "*") Modifier.offset(y = 7.dp) else Modifier,
                )
            }
            Box(Modifier.height(12.dp * k), contentAlignment = Alignment.TopCenter) {
                if (letters.isNotEmpty()) {
                    val plus = letters == "+"
                    BasicText(
                        letters,
                        style = IosType.keypadLetters.copy(
                            color = content,
                            fontSize = if (plus) 15.sp else 10.sp,
                            fontWeight = if (plus) FontWeight.Normal else FontWeight.Bold,
                            letterSpacing = if (plus) 0.sp else 2.5.sp,
                        ),
                        modifier = if (plus) Modifier.offset(y = (-3).dp) else Modifier,
                    )
                }
            }
        }
    }
}
