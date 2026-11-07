package com.iosphone.app.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.telecom.SimInfo
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.ui.theme.autoDir
import com.iosphone.app.ui.theme.ltr
import com.iosphone.app.util.PhoneFormat

object SimPicker {
    var sims by mutableStateOf<List<SimInfo>>(emptyList())
        private set
    var number by mutableStateOf<String?>(null)
        private set

    fun show(list: List<SimInfo>, target: String) {
        sims = list
        number = target
    }

    fun dismiss() {
        number = null
    }
}

/**
 * iOS 26 style "choose a SIM" sheet: the whole screen behind is blurred and dimmed (ScreenBlur), and the sheet
 * itself is a glass surface (BlurView when a BlurTarget is available, translucent fill otherwise).
 */
@Composable
fun SimPickerHost() {
    val context = LocalContext.current
    val colors = Ios.colors
    val visible = remember { MutableTransitionState(false) }
    val open = SimPicker.number != null
    visible.targetState = open
    var lastSims by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
    var lastNumber by remember { mutableStateOf("") }
    SimPicker.number?.let {
        lastNumber = it
        lastSims = SimPicker.sims
    }
    DisposableEffect(open) {
        if (open) ScreenBlur.count++
        onDispose { if (open) ScreenBlur.count-- }
    }
    if (!visible.currentState && !visible.targetState) return

    val sheetTint = if (colors.dark) Color(0x8C1F1F23) else Color(0x99FFFFFF)
    val sheetShape = RoundedCornerShape(34.dp)
    val edge = Brush.verticalGradient(
        if (colors.dark) listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))
        else listOf(Color.White, Color.Black.copy(alpha = 0.06f)),
    )

    BackHandler(enabled = open) { SimPicker.dismiss() }
    Box(Modifier.fillMaxSize()) {
        // Catches taps outside the sheet; the blur/dim itself comes from ScreenBlur.
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { SimPicker.dismiss() } })
        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.84f, stiffness = 380f)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 10.dp, top = 12.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                GlassBox(sheetShape, Modifier.fillMaxWidth(), blurRadius = 32f, overlay = sheetTint) {
                    Column(Modifier.fillMaxWidth().border(0.8.dp, edge, sheetShape)) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp, start = 20.dp, end = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BasicText(
                                stringResource(R.string.choose_sim),
                                style = IosType.footnote.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
                            )
                            if (lastNumber.isNotEmpty() && lastNumber != VOICEMAIL) {
                                Spacer(Modifier.height(2.dp))
                                BasicText(
                                    PhoneFormat.format(lastNumber),
                                    style = IosType.title3.copy(color = colors.label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center).ltr(),
                                    maxLines = 1,
                                )
                            }
                        }
                        lastSims.forEachIndexed { i, sim ->
                            Box(Modifier.padding(start = 20.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
                            SimRow(sim) {
                                placeCall(context, lastNumber, sim.handle)
                                SimPicker.dismiss()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassBox(
                    RoundedCornerShape(30.dp),
                    Modifier.fillMaxWidth().height(60.dp).pressable(pressedScale = 0.97f) { SimPicker.dismiss() },
                    contentAlignment = Alignment.Center,
                    blurRadius = 32f,
                    overlay = sheetTint,
                ) {
                    BasicText(
                        stringResource(R.string.cancel),
                        style = IosType.headline.copy(color = colors.tint, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimRow(sim: SimInfo, onClick: () -> Unit) {
    val colors = Ios.colors
    val base = Color(sim.color)
    val slotText = stringResource(R.string.sim_n, sim.slot + 1)
    val number = sim.number?.let { PhoneFormat.format(it) }
    Row(
        Modifier.fillMaxWidth().height(72.dp).iosRow(onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.verticalGradient(listOf(base.copy(alpha = 0.95f), base.copy(alpha = 0.70f)))),
            contentAlignment = Alignment.Center,
        ) {
            Image(Icons.Filled.SimCard, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                if (sim.label == slotText) slotText else sim.label,
                style = IosType.body.copy(color = colors.label, fontWeight = FontWeight.SemiBold).autoDir(),
                maxLines = 1,
            )
            val sub = listOfNotNull(slotText.takeIf { sim.label != slotText }, number).joinToString("  ·  ")
            if (sub.isNotEmpty()) {
                BasicText(sub, style = IosType.footnote.copy(color = colors.secondaryLabel).autoDir(), maxLines = 1)
            }
        }
        Image(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(colors.secondaryLabel.copy(alpha = 0.6f)),
        )
    }
}
