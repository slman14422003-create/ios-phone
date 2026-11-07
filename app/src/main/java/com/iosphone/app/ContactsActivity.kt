package com.iosphone.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iosphone.app.data.AppSettings
import com.iosphone.app.util.LocaleHelper
import com.iosphone.app.ui.shell.ContactsShell
import com.iosphone.app.ui.theme.IosTheme

class ContactsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            IosTheme(syncSystemBars = true) { ContactsShell() }
        }
    }
}
