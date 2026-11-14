package com.iosphone.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.telecom.SimInfo
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
    val target = SimPicker.number ?: return
    val sims = SimPicker.sims
    val items = sims.map { sim ->
        val slotText = stringResource(R.string.sim_n, sim.slot + 1)
        val label = if (sim.label == slotText) slotText else sim.label
        val sub = sim.number?.let { PhoneFormat.format(it) }
        MenuItem(
            label = if (sub.isNullOrEmpty()) label else "$label  ·  $sub",
            icon = Icons.Filled.SimCard,
        ) { placeCall(context, target, sim.handle) }
    }
    GlassMenu(
        expanded = true,
        onDismiss = { SimPicker.dismiss() },
        items = items,
        width = 290.dp,
    )
}
