package com.iosphone.app

import android.content.Intent
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
import com.iosphone.app.data.AppSettings
import com.iosphone.app.ui.common.LocalBlurTarget
import com.iosphone.app.ui.shell.PhoneChrome
import com.iosphone.app.ui.shell.PhoneContent
import com.iosphone.app.ui.shell.ShellViewModel
import com.iosphone.app.ui.theme.IosTheme
import eightbitlab.com.blurview.BlurTarget

class MainActivity : ComponentActivity() {
    private val vm: ShellViewModel by viewModels()
    private var dialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        enableEdgeToEdge()
        dialNumber = numberFrom(intent)
        if (dialNumber != null) showKeypad()

        // Two layers: all the scrolling content sits inside a BlurTarget, and the glass bar floats above it
        // as a sibling so BlurView can blur whatever scrolls underneath.
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
        setContentView(root)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dialNumber = numberFrom(intent)
        if (dialNumber != null) showKeypad()
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
}
