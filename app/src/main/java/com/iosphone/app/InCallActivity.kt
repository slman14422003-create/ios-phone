package com.iosphone.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.telecom.Call
import com.iosphone.app.data.AppSettings
import com.iosphone.app.util.LocaleHelper
import com.iosphone.app.telecom.CallManager
import com.iosphone.app.ui.incall.InCallScreen
import com.iosphone.app.ui.theme.IosTheme

class InCallActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        handle(intent)
        setContent {
            IosTheme {
                InCallScreen(
                    onFinish = { finish() },
                    onAddCall = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    onMessage = { number ->
                        runCatching {
                            startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    @Suppress("DEPRECATION")
    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_ANSWER) {
            val call = CallManager.primary()
            if (call != null && call.state == Call.STATE_RINGING) CallManager.answer(call)
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.iosphone.app.action.ANSWER"
    }
}
