package com.iosphone.app.ui.common

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.ui.theme.Ios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AvatarCache {
    private val cache = LruCache<String, ImageBitmap>(96)
    fun get(key: String?): ImageBitmap? = if (key == null) null else cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

fun initialsOf(name: String): String {
    val words = name.trim().split(' ').filter { it.isNotEmpty() }
    val letters = words.mapNotNull { w -> w.firstOrNull { it.isLetter() } }
    return when {
        letters.isEmpty() -> ""
        letters.size == 1 -> letters[0].uppercaseChar().toString()
        else -> "" + letters.first().uppercaseChar() + letters.last().uppercaseChar()
    }
}

@Composable
fun Avatar(name: String, photoUri: String?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = Ios.colors
    val targetPx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(AvatarCache.get(photoUri), photoUri) {
        if (photoUri == null) {
            value = null
        } else {
            val cached = AvatarCache.get(photoUri)
            if (cached != null) {
                value = cached
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        val uri = Uri.parse(photoUri)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        var sample = 1
                        while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
                        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, opts)
                        }?.asImageBitmap()
                    }.getOrNull()
                }
                if (loaded != null) AvatarCache.put(photoUri, loaded)
                value = loaded
            }
        }
    }
    val bmp = bitmap
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(colors.avatarTop, colors.avatarBottom))),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(bmp, null, Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            val text = initialsOf(name)
            if (text.isEmpty()) {
                Image(
                    Icons.Filled.Person,
                    null,
                    Modifier.size(size * 0.6f),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            } else {
                BasicText(
                    text,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
