package com.iosphone.app.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

const val VOICEMAIL = "voicemail"

fun placeCall(context: Context, number: String) {
    val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = if (number == VOICEMAIL) {
        Uri.fromParts("voicemail", "", null)
    } else {
        Uri.fromParts("tel", number, null)
    }
    tm.placeCall(uri, Bundle())
}

@Composable
fun rememberCaller(): (String) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pending
        pending = null
        if (granted && number != null) placeCall(context, number)
    }
    return remember(context, launcher) {
        { number: String ->
            if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                placeCall(context, number)
            } else {
                pending = number
                launcher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
}
