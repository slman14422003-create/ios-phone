package com.iosphone.app.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalDensity
import com.iosphone.app.data.AppSettings
import com.iosphone.app.telecom.SimManager
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
private fun simItems(sims: List<SimInfo>, target: String): List<MenuItem> {
    val context = LocalContext.current
    return sims.map { sim ->
        val slotText = stringResource(R.string.sim_n, sim.slot + 1)
        val label = if (sim.label == slotText) slotText else sim.label
        val sub = sim.number?.let { PhoneFormat.format(it) }
        MenuItem(
            label = if (sub.isNullOrEmpty()) label else "$label  ·  $sub",
            icon = Icons.Filled.SimCard,
        ) { placeCall(context, target, sim.handle) }
    }
}

@Composable
fun SimPickerHost() {
    val target = SimPicker.number ?: return
    GlassMenu(
        expanded = true,
        onDismiss = { SimPicker.dismiss() },
        items = simItems(SimPicker.sims, target),
        width = 250.dp,
    )
}

@Composable
fun SimMenu(expanded: Boolean, onDismiss: () -> Unit, sims: List<SimInfo>, number: String, title: String?) {
    if (!expanded) return
    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
    GlassMenu(
        expanded = true,
        onDismiss = onDismiss,
        items = simItems(sims, number),
        width = 220.dp,
        positionProvider = ClampedBelowProvider(gap),
        header = title ?: PhoneFormat.format(number),
    )
}

fun simChoices(context: Context): List<SimInfo>? {
    if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return null
    val sims = SimManager.list(context)
    if (sims.size < 2) return null
    return if (SimManager.find(sims, AppSettings.defaultSim) != null) null else sims
}
