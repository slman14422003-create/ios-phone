package com.iosphone.app.ui.incall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.CallBgSettings
import com.iosphone.app.ui.common.CallBackdrop

@Composable
private fun previewUi(incoming: Boolean): CallUi = CallUi(
    name = stringResource(R.string.preview_name),
    number = "+1 555 0100",
    photo = null,
    statusLabel = stringResource(if (incoming) R.string.mobile else R.string.preview_timer),
    connectTime = 0L,
    sim = stringResource(R.string.sim_n, 1),
    mode = if (incoming) CallMode.Ringing else CallMode.Controls,
    typed = "",
    speakerOn = false,
    muted = false,
    onHold = false,
    finished = false,
    others = emptyList(),
    canMerge = false,
)

@Composable
fun CallPreview(cfg: CallBgSettings, incoming: Boolean, modifier: Modifier = Modifier) {
    val look = cfg.look
    val palette = remember(look) { callPalette(look) }
    val context = LocalContext.current
    val ui = previewUi(incoming)
    BoxWithConstraints(modifier.aspectRatio(360f / 700f).clip(RoundedCornerShape(18.dp))) {
        val scale = maxWidth.value / 360f
        CallBackdrop(look, cfg.file(context), null, Modifier.fillMaxSize(), blurScale = 0.5f)
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center, unbounded = true)
                .requiredSize(360.dp, 700.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            CallContent(ui, CallActions(), look, palette, insets = false)
        }
    }
}

@Composable
fun CallPreviewFull(cfg: CallBgSettings, incoming: Boolean) {
    val look = cfg.look
    val palette = remember(look) { callPalette(look) }
    val context = LocalContext.current
    val ui = previewUi(incoming)
    Box(Modifier.fillMaxSize()) {
        CallBackdrop(look, cfg.file(context), null, Modifier.fillMaxSize())
        CallContent(ui, CallActions(), look, palette, insets = true)
    }
}
