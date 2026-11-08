package com.iosphone.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Soft fades under the status bar and the gesture area so scrolling content never collides with the system UI. */
@Composable
fun BoxScope.EdgeScrims(base: Color) {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    Box0(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(top + 12.dp)
            .background(
                Brush.verticalGradient(
                    0f to base.copy(alpha = 0.97f),
                    0.7f to base.copy(alpha = 0.9f),
                    1f to Color.Transparent,
                ),
            ),
    )
    Box0(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(bottom + 44.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, base.copy(alpha = 0.92f)))),
    )
}

@Composable
private fun Box0(modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier)
}
