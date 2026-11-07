package com.iosphone.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.iosphone.app.data.BG_COLORS
import com.iosphone.app.data.BG_PHOTO
import com.iosphone.app.data.CallLook
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackdropPreset(val top: Int, val bottom: Int, val blobs: IntArray)

val BackdropPresets = listOf(
    BackdropPreset(0xFF2B3649.toInt(), 0xFF141A25.toInt(), intArrayOf(0xFF4A6FA5.toInt(), 0xFF2D3F63.toInt(), 0xFF5B4B8A.toInt())),
    BackdropPreset(0xFF1F5F8B.toInt(), 0xFF071B2E.toInt(), intArrayOf(0xFF2EC4B6.toInt(), 0xFF3A86FF.toInt(), 0xFF1B4965.toInt())),
    BackdropPreset(0xFFFF7E5F.toInt(), 0xFF3A1C5C.toInt(), intArrayOf(0xFFFFB347.toInt(), 0xFFFF4D6D.toInt(), 0xFF7B2CBF.toInt())),
    BackdropPreset(0xFF0B3D3A.toInt(), 0xFF120C2B.toInt(), intArrayOf(0xFF2DD4A7.toInt(), 0xFF7C4DFF.toInt(), 0xFF1FA2FF.toInt())),
    BackdropPreset(0xFFB24A6B.toInt(), 0xFF2A0D1B.toInt(), intArrayOf(0xFFFF8FAB.toInt(), 0xFFC9184A.toInt(), 0xFF6D2E8F.toInt())),
    BackdropPreset(0xFF1E5B4A.toInt(), 0xFF08191A.toInt(), intArrayOf(0xFF52B788.toInt(), 0xFF2D6A4F.toInt(), 0xFF40916C.toInt())),
    BackdropPreset(0xFF3A3A3C.toInt(), 0xFF0A0A0B.toInt(), intArrayOf(0xFF636366.toInt(), 0xFF48484A.toInt(), 0xFF8E8E93.toInt())),
    BackdropPreset(0xFF6C5CE7.toInt(), 0xFF1B1464.toInt(), intArrayOf(0xFFA29BFE.toInt(), 0xFF6C5CE7.toInt(), 0xFFFD79A8.toInt())),
    BackdropPreset(0xFF16A085.toInt(), 0xFF0B2B2A.toInt(), intArrayOf(0xFF55EFC4.toInt(), 0xFF00CEC9.toInt(), 0xFF0984E3.toInt())),
    BackdropPreset(0xFFE17055.toInt(), 0xFF2D0F0A.toInt(), intArrayOf(0xFFFAB1A0.toInt(), 0xFFD63031.toInt(), 0xFFFDCB6E.toInt())),
    BackdropPreset(0xFF74B9FF.toInt(), 0xFF0C2461.toInt(), intArrayOf(0xFFDFF9FB.toInt(), 0xFF0984E3.toInt(), 0xFF81ECEC.toInt())),
    BackdropPreset(0xFF8E2DE2.toInt(), 0xFF1F0A3C.toInt(), intArrayOf(0xFFE056FD.toInt(), 0xFF4A00E0.toInt(), 0xFFFF6B9D.toInt())),
    BackdropPreset(0xFFB8860B.toInt(), 0xFF1A1205.toInt(), intArrayOf(0xFFFFD166.toInt(), 0xFFE09F3E.toInt(), 0xFF9E2A2B.toInt())),
    BackdropPreset(0xFF1C1C1E.toInt(), 0xFF000000.toInt(), intArrayOf(0xFF2C2C2E.toInt(), 0xFF3A3A3C.toInt(), 0xFF1C1C1E.toInt())),
)

class BackdropColors(val stops: List<Color>, val blobs: List<Color>)

fun backdropColors(look: CallLook): BackdropColors {
    if (look.bg == BG_COLORS) {
        val a = Color(look.c1)
        val b = Color(look.c2)
        val c = Color(look.c3)
        return BackdropColors(listOf(a, b, c), listOf(b, a, b))
    }
    val p = BackdropPresets[if (look.bg in BackdropPresets.indices) look.bg else 0]
    return BackdropColors(listOf(Color(p.top), Color(p.bottom)), p.blobs.map { Color(it) })
}

private val blobSpots = arrayOf(
    floatArrayOf(0.15f, 0.18f, 0.75f),
    floatArrayOf(0.88f, 0.48f, 0.8f),
    floatArrayOf(0.3f, 0.88f, 0.85f),
)

private val noiseTile: androidx.compose.ui.graphics.ImageBitmap by lazy {
    val size = 96
    val rnd = java.util.Random(7)
    val pixels = IntArray(size * size) {
        val g = 96 + rnd.nextInt(64)
        (0xFF shl 24) or (g shl 16) or (g shl 8) or g
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
private fun GradientLayer(look: CallLook, modifier: Modifier = Modifier) {
    val colors = remember(look.bg, look.c1, look.c2, look.c3) { backdropColors(look) }
    val phase: State<Float>? = if (look.animated) {
        rememberInfiniteTransition(label = "backdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
    } else {
        null
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val theta = look.angle * PI.toFloat() / 180f
        val dx = sin(theta)
        val dy = -cos(theta)
        val length = abs(w * dx) + abs(h * dy)
        val start = Offset(w / 2f - dx * length / 2f, h / 2f - dy * length / 2f)
        val end = Offset(w / 2f + dx * length / 2f, h / 2f + dy * length / 2f)
        drawRect(Brush.linearGradient(colors.stops, start, end))
        val t = phase?.value ?: 0f
        val spread = 0.8f + look.blur * 0.5f
        for (i in colors.blobs.indices) {
            val spot = blobSpots[i % blobSpots.size]
            val turn = 2f * PI.toFloat() * (t + i / 3f)
            val cx = (spot[0] + if (phase != null) 0.1f * sin(turn) else 0f) * w
            val cy = (spot[1] + if (phase != null) 0.08f * cos(turn) else 0f) * h
            val radius = spot[2] * w * spread
            val c = colors.blobs[i]
            drawRect(
                Brush.radialGradient(
                    listOf(c.copy(alpha = 0.69f * look.glow), c.copy(alpha = 0f)),
                    Offset(cx, cy),
                    radius,
                ),
            )
        }
    }
}

@Composable
private fun FinishLayer(look: CallLook, modifier: Modifier = Modifier) {
    val brush = remember { ShaderBrush(ImageShader(noiseTile, TileMode.Repeated, TileMode.Repeated)) }
    Canvas(modifier) {
        if (look.vignette > 0f) {
            drawRect(
                Brush.radialGradient(
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.7f * look.vignette),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = hypot(size.width, size.height) / 2f,
                ),
            )
        }
        if (look.grain > 0f) {
            drawRect(brush, alpha = look.grain * 0.5f, blendMode = BlendMode.Overlay)
        }
        if (look.dim > 0f) drawRect(Color.Black.copy(alpha = look.dim))
    }
}

private fun decodeSampled(open: () -> java.io.InputStream?, limit: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= limit) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return open()?.use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun loadBackdropBitmap(context: Context, look: CallLook, photoFile: File, contactPhoto: String?): Bitmap? {
    if (look.contactPhoto && contactPhoto != null) {
        val b = runCatching {
            decodeSampled({ context.contentResolver.openInputStream(Uri.parse(contactPhoto)) }, 1400)
        }.getOrNull()
        if (b != null) return b
    }
    if (look.bg == BG_PHOTO && photoFile.exists()) {
        return runCatching { decodeSampled({ photoFile.inputStream() }, 1600) }.getOrNull()
    }
    return null
}

@Composable
fun CallBackdrop(
    look: CallLook,
    photoFile: File,
    contactPhoto: String?,
    modifier: Modifier = Modifier,
    blurScale: Float = 1f,
) {
    val context = LocalContext.current
    val wantsPhoto = (look.contactPhoto && contactPhoto != null) || look.bg == BG_PHOTO
    val bitmap by produceState<Bitmap?>(null, wantsPhoto, look.contactPhoto, look.bg, look.photoVersion, contactPhoto) {
        value = if (wantsPhoto) withContext(Dispatchers.IO) { loadBackdropBitmap(context, look, photoFile, contactPhoto) } else null
    }
    val radius = max(0.5f, look.blur * 40f * blurScale)
    Box(modifier) {
        GradientLayer(look, Modifier.fillMaxSize())
        val shown = bitmap
        if (shown != null) {
            val fade = remember { Animatable(0f) }
            LaunchedEffect(Unit) { fade.animateTo(1f, tween(350)) }
            AndroidView(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value },
                factory = { ctx ->
                    val root = FrameLayout(ctx)
                    val target = BlurTarget(ctx)
                    val image = ImageView(ctx)
                    image.scaleType = ImageView.ScaleType.CENTER_CROP
                    target.addView(image, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    val blur = BlurView(ctx)
                    root.addView(target, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    root.addView(blur, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    blur.setupWith(target, 2f, true).setBlurRadius(radius)
                    root
                },
                update = { root ->
                    val target = root.getChildAt(0) as FrameLayout
                    val image = target.getChildAt(0) as ImageView
                    val blur = root.getChildAt(1) as BlurView
                    if (image.tag !== shown) {
                        image.setImageBitmap(shown)
                        image.tag = shown
                    }
                    blur.setBlurRadius(radius)
                },
            )
        }
        FinishLayer(look, Modifier.fillMaxSize())
    }
}
