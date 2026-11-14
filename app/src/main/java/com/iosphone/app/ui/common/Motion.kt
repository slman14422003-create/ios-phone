package com.iosphone.app.ui.common

import androidx.compose.animation.core.CubicBezierEasing

object Motion {
    const val PushMs = 420
    const val MenuMs = 240
    const val Parallax = 0.3333f
    const val PushDim = 0.2f
    const val MenuBlur = 40f
    const val MenuDim = 0.3f
    val PushEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val MenuEase = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)
}
