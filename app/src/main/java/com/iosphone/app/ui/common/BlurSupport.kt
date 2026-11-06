package com.iosphone.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.iosphone.app.data.BG_CUSTOM
import com.iosphone.app.data.CallBgSettings
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class BackdropPreset(val name: String, val top: Int, val bottom: Int, val blobs: IntArray)

/** Built-in call backgrounds. Each is a gradient with a few soft colour blobs so the blur has something to work on. */
val BackdropPresets = listOf(
    BackdropPreset("Midnight", 0xFF2B3649.toInt(), 0xFF141A25.toInt(), intArrayOf(0xFF4A6FA5.toInt(), 0xFF2D3F63.toInt(), 0xFF5B4B8A.toInt())),
    BackdropPreset("Ocean", 0xFF1F5F8B.toInt(), 0xFF071B2E.toInt(), intArrayOf(0xFF2EC4B6.toInt(), 0xFF3A86FF.toInt(), 0xFF1B4965.toInt())),
    BackdropPreset("Sunset", 0xFFFF7E5F.toInt(), 0xFF3A1C5C.toInt(), intArrayOf(0xFFFFB347.toInt(), 0xFFFF4D6D.toInt(), 0xFF7B2CBF.toInt())),
    BackdropPreset("Aurora", 0xFF0B3D3A.toInt(), 0xFF120C2B.toInt(), intArrayOf(0xFF2DD4A7.toInt(), 0xFF7C4DFF.toInt(), 0xFF1FA2FF.toInt())),
    BackdropPreset("Rose", 0xFFB24A6B.toInt(), 0xFF2A0D1B.toInt(), intArrayOf(0xFFFF8FAB.toInt(), 0xFFC9184A.toInt(), 0xFF6D2E8F.toInt())),
    BackdropPreset("Forest", 0xFF1E5B4A.toInt(), 0xFF08191A.toInt(), intArrayOf(0xFF52B788.toInt(), 0xFF2D6A4F.toInt(), 0xFF40916C.toInt())),
    BackdropPreset("Graphite", 0xFF3A3A3C.toInt(), 0xFF0A0A0B.toInt(), intArrayOf(0xFF636366.toInt(), 0xFF48484A.toInt(), 0xFF8E8E93.toInt())),
)

fun renderPreset(p: BackdropPreset): Bitmap {
    val w = 360
    val h = 780
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), p.top, p.bottom, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    val spots = arrayOf(
        floatArrayOf(0.15f, 0.18f, 0.75f),
        floatArrayOf(0.88f, 0.48f, 0.8f),
        floatArrayOf(0.3f, 0.88f, 0.85f),
    )
    for (i in p.blobs.indices) {
        val spot = spots[i % spots.size]
        val col = p.blobs[i]
        val clear = col and 0x00FFFFFF
        val strong = clear or 0xB0000000.toInt()
        paint.shader = RadialGradient(spot[0] * w, spot[1] * h, spot[2] * w, strong, clear, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }
    paint.shader = null
    return bmp
}

private fun backdropBitmap(context: Context, cfg: CallBgSettings): Bitmap {
    if (cfg.preset == BG_CUSTOM) {
        val f = cfg.file(context)
        if (f.exists()) {
            val decoded = runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
            if (decoded != null) return decoded
        }
    }
    val index = if (cfg.preset in BackdropPresets.indices) cfg.preset else 0
    return renderPreset(BackdropPresets[index])
}

/**
 * Full-bleed call background: the chosen gradient/photo, blurred by BlurView, then dimmed.
 * [blurScale] lets small previews use a proportionally weaker blur.
 */
@Composable
fun CallBackdrop(cfg: CallBgSettings, modifier: Modifier = Modifier, blurScale: Float = 1f) {
    val context = LocalContext.current
    val bitmap = remember(cfg.preset, cfg.customVersion) { backdropBitmap(context, cfg) }
    val radius = maxOf(0.5f, cfg.blur * 40f * blurScale)
    val overlay = android.graphics.Color.argb((cfg.dim * 255f).toInt(), 0, 0, 0)
    AndroidView(
        modifier = modifier,
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
            if (image.tag !== bitmap) {
                image.setImageBitmap(bitmap)
                image.tag = bitmap
            }
            blur.setBlurRadius(radius)
            blur.setOverlayColor(overlay)
        },
    )
}
