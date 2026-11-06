package com.iosphone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iosphone.app.ui.shell.PhoneShell
import com.iosphone.app.ui.theme.IosTheme

class MainActivity : ComponentActivity() {
    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dialNumber = numberFrom(intent)
        setContent {
            IosTheme { PhoneShell(dialNumber) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dialNumber = numberFrom(intent)
    }

    private fun numberFrom(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "tel") return null
        return uri.schemeSpecificPart?.takeIf { it.isNotBlank() }
    }
}
