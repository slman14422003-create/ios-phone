package com.iosphone.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/** Counts open menus; PhoneContent blurs and dims the whole screen while it is above zero. */
object ScreenBlur {
    var count by mutableIntStateOf(0)
}

fun Modifier.menuSurface(shape: Shape): Modifier = composed {
    val c = Ios.colors
    this
        .shadow(
            elevation = 24.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (c.dark) 0.5f else 0.12f),
            spotColor = Color.Black.copy(alpha = if (c.dark) 0.6f else 0.22f),
        )
        .clip(shape)
        .background(if (c.dark) Color(0xF2262628) else Color(0xF5F7F7F9))
        .border(0.5.dp, c.glassBorder, shape)
}

/** Set only for UI that sits *above* a BlurTarget (e.g. the bottom bar). When null, glass falls back to a translucent fill. */
val LocalBlurTarget = staticCompositionLocalOf<BlurTarget?> { null }

/**
 * iOS-style glass surface. With a [LocalBlurTarget] it blurs whatever is scrolling underneath
 * using BlurView; otherwise it draws the plain translucent fill.
 */
@Composable
fun GlassBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Ios.colors
    val target = LocalBlurTarget.current
    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (c.dark) 0.35f else 0.08f),
                spotColor = Color.Black.copy(alpha = if (c.dark) 0.5f else 0.18f),
            )
            .clip(shape)
            .border(0.5.dp, c.glassBorder, shape),
        contentAlignment = contentAlignment,
    ) {
        if (target != null) {
            val tint = c.blurTint.toArgb()
            AndroidView(
                factory = { ctx ->
                    BlurView(ctx).also { v ->
                        v.setupWith(target, 2f, false).setBlurRadius(32f)
                        v.setOverlayColor(tint)
                    }
                },
                update = { v -> v.setOverlayColor(tint) },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(Modifier.matchParentSize().background(c.glass))
        }
        content()
    }
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
    GlassBox(
        shape = CircleShape,
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(tint ?: colors.label))
    }
}

@Composable
fun GlassTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, bold: Boolean = false) {
    val colors = Ios.colors
    GlassBox(
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .height(44.dp)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = (if (bold) IosType.headline else IosType.body).copy(color = accent ?: colors.label),
            modifier = Modifier.padding(horizontal = 18.dp),
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
    DisposableEffect(expanded) {
        if (expanded) ScreenBlur.count++
        onDispose { if (expanded) ScreenBlur.count-- }
    }
    if (!expanded) return
    val colors = Ios.colors
    val density = LocalDensity.current
    val offset = with(density) { IntOffset(0, offsetY.roundToPx()) }
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val origin = if (alignment == Alignment.BottomCenter) TransformOrigin(0.5f, 1f) else TransformOrigin(1f, 0f)
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(140)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                initialScale = 0.8f,
                transformOrigin = origin,
            ),
            exit = fadeOut(tween(90)),
        ) {
            Column(Modifier.width(width).menuSurface(RoundedCornerShape(24.dp)).padding(vertical = 8.dp)) {
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
}
