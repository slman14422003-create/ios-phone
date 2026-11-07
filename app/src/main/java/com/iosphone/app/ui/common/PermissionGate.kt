package com.iosphone.app.ui.common

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

val APP_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.WRITE_CALL_LOG,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.ANSWER_PHONE_CALLS,
    Manifest.permission.POST_NOTIFICATIONS,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_PHONE_NUMBERS,
    Manifest.permission.BLUETOOTH_CONNECT,
)

@Composable
fun RequestPermissionsOnce() {
    val context = LocalContext.current
    val data = LocalAppData.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        data.invalidate()
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!asked) {
            asked = true
            val missing = APP_PERMISSIONS.filter {
                context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
        }
    }
}

object DefaultDialer {
    fun isDefault(context: Context): Boolean {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return rm.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    fun requestIntent(context: Context): Intent {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    }
}

@Composable
fun PermissionGate(
    permission: String,
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = Ios.colors
    val data = LocalAppData.current
    val version = data.version
    var granted by remember(version) {
        mutableStateOf(context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        data.invalidate()
    }
    if (granted) {
        content()
        return
    }
    Column(
        Modifier.fillMaxSize().background(colors.background).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            title,
            style = IosType.title3.copy(
                color = colors.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            body,
            style = IosType.body.copy(
                color = colors.secondaryLabel,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(colors.tint)
                .clickable { launcher.launch(permission) },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.allow_access),
                style = IosType.headline.copy(color = Color.White),
            )
        }
        Spacer(Modifier.height(16.dp))
        BasicText(
            stringResource(R.string.open_settings),
            style = IosType.body.copy(color = colors.tint),
            modifier = Modifier.clickable {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            },
        )
    }
}
