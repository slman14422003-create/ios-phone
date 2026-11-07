package com.iosphone.app.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
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

fun placeCall(context: Context, number: String, handle: PhoneAccountHandle? = null) {
    val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = if (number == VOICEMAIL) {
        Uri.fromParts("voicemail", "", null)
    } else {
        Uri.fromParts("tel", number, null)
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
