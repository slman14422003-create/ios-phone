package com.iosphone.app.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.iosphone.app.data.AppSettings
import com.iosphone.app.telecom.SimManager

const val VOICEMAIL = "voicemail"

private fun roamingNumber(context: Context, number: String, handle: PhoneAccountHandle?): String {
    if (!AppSettings.roamingAssist) return number
    if (number.length < 6 || number.startsWith("+") || number.startsWith("00") || number.any { it == '*' || it == '#' }) return number
    return runCatching {
        val base = context.getSystemService(TelephonyManager::class.java)
        val t = if (handle != null) base.createForPhoneAccountHandle(handle) ?: base else base
        if (!t.isNetworkRoaming) return@runCatching number
        val iso = t.simCountryIso.uppercase()
        if (iso.isEmpty()) number else PhoneNumberUtils.formatNumberToE164(number, iso) ?: number
    }.getOrDefault(number)
}

fun placeCall(context: Context, number: String, handle: PhoneAccountHandle? = null) {
    val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = if (number == VOICEMAIL) {
        Uri.fromParts("voicemail", "", null)
    } else {
        Uri.fromParts("tel", roamingNumber(context, number, handle), null)
    }
    val extras = Bundle()
    if (handle != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
    runCatching { tm.placeCall(uri, extras) }
}

fun dispatchCall(context: Context, number: String, preferred: PhoneAccountHandle?) {
    val sims = SimManager.list(context)
    if (sims.size < 2) {
        placeCall(context, number, preferred)
        return
    }
    val chosen = preferred ?: SimManager.find(sims, AppSettings.defaultSim)?.handle
    if (chosen != null) placeCall(context, number, chosen) else SimPicker.show(sims, number)
}

@Composable
fun rememberCaller(simOverride: () -> PhoneAccountHandle? = { null }): (String) -> Unit {
    val context = LocalContext.current
    val override by rememberUpdatedState(simOverride)
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pending
        pending = null
        if (granted && number != null) dispatchCall(context, number, override())
    }
    return remember(context, launcher) {
        { number: String ->
            if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                dispatchCall(context, number, override())
            } else {
                pending = number
                launcher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
}
