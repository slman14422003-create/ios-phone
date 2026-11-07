package com.iosphone.app.ui.common

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.ui.theme.IosType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun PadKey(
    digit: String,
    letters: String,
    size: Dp,
    fill: Color,
    pressedFill: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onLong: (() -> Unit)? = null,
) {
    val level = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val fade = remember { arrayOfNulls<Job>(1) }
    val press = rememberUpdatedState(onPress)
    val long = rememberUpdatedState(onLong)
    val k = size.value / 80f
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(fill)
                val a = level.floatValue
                if (a > 0f) drawCircle(pressedFill, alpha = a)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    fade[0]?.cancel()
                    level.floatValue = 1f
                    press.value()
                    val longAction = long.value
                    if (longAction != null) {
                        var released = false
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                            released = true
                        }
                        if (!released) {
                            longAction()
                            waitForUpOrCancellation()
                        }
                    } else {
                        waitForUpOrCancellation()
                    }
                    fade[0] = scope.launch {
                        animate(level.floatValue, 0f, animationSpec = tween(380)) { v, _ -> level.floatValue = v }
                    }
                }
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
