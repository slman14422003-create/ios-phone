package com.iosphone.app.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

fun Modifier.glass(shape: Shape): Modifier = composed {
    val c = Ios.colors
    this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (c.dark) 0.35f else 0.08f),
            spotColor = Color.Black.copy(alpha = if (c.dark) 0.5f else 0.18f),
        )
        .clip(shape)
        .background(c.glass)
        .border(0.5.dp, c.glassBorder, shape)
}

fun Modifier.pressable(enabled: Boolean = true, pressedScale: Float = 0.93f, onClick: () -> Unit): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    this
        .scale(s)
        .pointerInput(enabled, onClick) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            }
        }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val colors = Ios.colors
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(enabled = enabled, onClick = onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(tint ?: colors.label))
    }
}

@Composable
fun GlassTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, bold: Boolean = false) {
    val colors = Ios.colors
    Box(
        modifier = modifier
            .height(44.dp)
            .pressable(onClick = onClick)
            .glass(RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = (if (bold) IosType.headline else IosType.body).copy(color = accent ?: colors.label),
        )
    }
}

class MenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val checked: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<MenuItem>,
    alignment: Alignment = Alignment.TopEnd,
    offsetY: Dp = 52.dp,
    width: Dp = 250.dp,
) {
    if (!expanded) return
    val colors = Ios.colors
    val density = LocalDensity.current
    val offset = with(density) { IntOffset(0, offsetY.roundToPx()) }
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(Modifier.width(width).glass(RoundedCornerShape(24.dp)).padding(vertical = 8.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressable(pressedScale = 0.98f) {
                            onDismiss()
                            item.onClick()
                        }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        if (item.checked) {
                            Image(Icons.Filled.Check, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.label))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        item.label,
                        style = IosType.body.copy(color = if (item.destructive) colors.red else colors.label),
                        modifier = Modifier.weight(1f),
                    )
                    if (item.icon != null) {
                        Image(
                            item.icon,
                            null,
                            Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(if (item.destructive) colors.red else colors.label),
                        )
                    }
                }
                if (index < items.lastIndex) {
                    Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
                }
            }
        }
    }
}
