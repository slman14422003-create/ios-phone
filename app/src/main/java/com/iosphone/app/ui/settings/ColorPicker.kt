package com.iosphone.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

private val quickColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFFFF453A,
    0xFFFF9F0A, 0xFFFFD60A, 0xFF30D158, 0xFF34C759, 0xFF64D2FF, 0xFF00F0FF, 0xFF8E8E93,
).map { it.toInt() }

private val hueColors = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }

@Composable
fun ColorField(title: String, color: Int, onChange: (Int) -> Unit) {
    val colors = Ios.colors
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).pressable(pressedScale = 0.99f) { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(title, style = IosType.body.copy(color = colors.label), modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(28.dp)
                    .border(2.dp, colors.label.copy(alpha = 0.25f), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(color)),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(bottom = 10.dp)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    quickColors.forEach { c ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .pressable(pressedScale = 0.9f) { onChange(c) }
                                .then(if (c == color) Modifier.border(3.dp, colors.tint, CircleShape) else Modifier)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(1.dp, colors.label.copy(alpha = 0.15f), CircleShape),
                        )
                    }
                }
                HsvPicker(color, onChange)
            }
        }
    }
}

@Composable
fun HsvPicker(color: Int, onChange: (Int) -> Unit) {
    val latest = rememberUpdatedState(onChange)
    val init = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var h by remember { mutableFloatStateOf(init[0]) }
    var s by remember { mutableFloatStateOf(init[1]) }
    var v by remember { mutableFloatStateOf(init[2]) }
    LaunchedEffect(color) {
        if (android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) != color) {
            val a = FloatArray(3)
            android.graphics.Color.colorToHSV(color, a)
            h = a[0]
            s = a[1]
            v = a[2]
        }
    }
    fun push() {
        latest.value(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(h, 1f, 1f))))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val c = Offset(s * this.size.width, (1f - v) * this.size.height)
                    drawCircle(Color.White, 11.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.25f), 12.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                }
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        s = (o.x / size.width).coerceIn(0f, 1f)
                        v = 1f - (o.y / size.height).coerceIn(0f, 1f)
                        push()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o ->
                            s = (o.x / size.width).coerceIn(0f, 1f)
                            v = 1f - (o.y / size.height).coerceIn(0f, 1f)
                            push()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            s = (change.position.x / size.width).coerceIn(0f, 1f)
                            v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            push()
                        },
                    )
                },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRect(Brush.horizontalGradient(hueColors))
                    val x = (h / 360f) * this.size.width
                    drawCircle(Color.White, 12.dp.toPx(), Offset(x, this.size.height / 2f), style = Stroke(3.dp.toPx()))
                }
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        h = (o.x / size.width).coerceIn(0f, 1f) * 360f
                        push()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o ->
                            h = (o.x / size.width).coerceIn(0f, 1f) * 360f
                            push()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                            push()
                        },
                    )
                },
        )
        Spacer(Modifier.height(2.dp))
    }
}
