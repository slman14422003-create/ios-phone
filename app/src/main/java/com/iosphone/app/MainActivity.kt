package com.iosphone.app

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.iosphone.app.data.AppData
import com.iosphone.app.data.AppSettings
import com.iosphone.app.telecom.Transitions
import com.iosphone.app.util.LocaleHelper
import com.iosphone.app.ui.common.LocalBlurTarget
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.SimPickerHost
import com.iosphone.app.ui.shell.PhoneChrome
import com.iosphone.app.ui.shell.PhoneContent
import com.iosphone.app.ui.shell.ShellViewModel
import com.iosphone.app.ui.theme.IosTheme
import eightbitlab.com.blurview.BlurTarget

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private val vm: ShellViewModel by viewModels()
    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        Transitions.install(this)
        AppData.warm(this)
        vm.nav.onEmpty = { finish() }
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        dialNumber = numberFrom(intent)
        if (dialNumber != null) showKeypad()
        if (savedInstanceState == null) routeFrom(intent)

        val root = FrameLayout(this)
        val target = BlurTarget(this)
        val content = ComposeView(this).apply {
            setContent { IosTheme(syncSystemBars = true) { PhoneContent(vm, dialNumber) } }
        }
        target.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(target, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val chrome = ComposeView(this).apply {
            setContent {
                IosTheme(syncSystemBars = true) {
                    CompositionLocalProvider(LocalBlurTarget provides target) { PhoneChrome(vm) }
                }
            }
        }
        root.addView(
            chrome,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM),
        )
        val overlay = ComposeView(this).apply {
            setContent {
                IosTheme(syncSystemBars = true) {
                    CompositionLocalProvider(LocalBlurTarget provides target) { SimPickerHost() }
                }
            }
        }
        root.addView(
            overlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        setContentView(root)
    }

    override fun onStop() {
        vm.nav.returnOnEmpty = false
        super.onStop()
    }

    override fun onDestroy() {
        vm.nav.onEmpty = null
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        Transitions.finished(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dialNumber = numberFrom(intent)
        if (dialNumber != null) showKeypad()
        routeFrom(intent)
    }

    /** Deep links from the Contacts app: a contact's recordings, or the call history / block screen of a number. */
    private fun routeFrom(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> if (intent.type == CALLS_TYPE) {
                vm.tab = 0
                vm.searching = false
                vm.query = ""
                vm.nav.stack.clear()
            }
            Intent.ACTION_CALL_BUTTON -> showKeypad()
            ACTION_RECORDINGS -> {
                val id = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
                vm.nav.stack.clear()
                vm.nav.returnOnEmpty = intent.getBooleanExtra(Transitions.EXTRA_RETURN, false)
                vm.nav.push(Route.Recordings(id.takeIf { it >= 0L }))
            }
            ACTION_CALL_INFO -> {
                val number = intent.getStringExtra(EXTRA_NUMBER)?.takeIf { it.isNotBlank() } ?: return
                vm.nav.stack.clear()
                vm.nav.returnOnEmpty = intent.getBooleanExtra(Transitions.EXTRA_RETURN, false)
                vm.nav.push(Route.CallInfo(number))
            }
        }
    }

    private fun showKeypad() {
        vm.tab = 2
        vm.searching = false
        vm.query = ""
    }

    private fun numberFrom(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "tel") return null
        return uri.schemeSpecificPart?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val CALLS_TYPE = "vnd.android.cursor.dir/calls"
        const val ACTION_RECORDINGS = "com.iosphone.app.action.RECORDINGS"
        const val ACTION_CALL_INFO = "com.iosphone.app.action.CALL_INFO"
        const val EXTRA_CONTACT_ID = "contact_id"
        const val EXTRA_NUMBER = "number"
    }
}
