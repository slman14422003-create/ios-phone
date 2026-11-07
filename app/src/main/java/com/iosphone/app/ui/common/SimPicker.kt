package com.iosphone.app.ui.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.iosphone.app.R
import com.iosphone.app.telecom.SimInfo
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
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

@Composable
fun SimPickerHost() {
    val context = LocalContext.current
    val colors = Ios.colors
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = SimPicker.number != null
    var lastSims by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
    var lastNumber by remember { mutableStateOf("") }
    SimPicker.number?.let {
        lastNumber = it
        lastSims = SimPicker.sims
    }
    if (!visible.currentState && !visible.targetState) return

    Dialog(
        onDismissRequest = { SimPicker.dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visibleState = visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f))
                        .pointerInput(Unit) { detectTapGestures { SimPicker.dismiss() } },
                )
            }
            AnimatedVisibility(
                visibleState = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) { it } + fadeIn(tween(160)),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
            ) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Column(Modifier.fillMaxWidth().menuSurface(RoundedCornerShape(28.dp))) {
                        BasicText(
                            stringResource(R.string.choose_sim),
                            style = IosType.footnote.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        )
                        lastSims.forEachIndexed { index, sim ->
                            Hairline(0.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(66.dp)
                                    .pressable(pressedScale = 0.98f) {
                                        placeCall(context, lastNumber, sim.handle)
                                        SimPicker.dismiss()
                                    }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(40.dp).background(colors.tint, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(Icons.Filled.SimCard, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Color.White))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    BasicText(
                                        sim.label,
                                        style = IosType.body.copy(color = colors.label, fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                    )
                                    val number = sim.number?.let { "  ·  " + PhoneFormat.format(it) }.orEmpty()
                                    BasicText(
                                        stringResource(R.string.sim_n, sim.slot + 1) + number,
                                        style = IosType.footnote.copy(color = colors.secondaryLabel),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .menuSurface(RoundedCornerShape(29.dp))
                            .pressable(pressedScale = 0.97f) { SimPicker.dismiss() },
                        contentAlignment = Alignment.Center,
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
}
