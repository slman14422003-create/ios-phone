package com.iosphone.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import com.iosphone.app.ui.common.SimPicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.data.rememberAppData
import com.iosphone.app.ui.common.EdgeScrims
import com.iosphone.app.ui.common.GlassSearchField
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Nav
import com.iosphone.app.ui.common.RequestPermissionsOnce
import com.iosphone.app.ui.common.RouteLayer
import com.iosphone.app.ui.common.SimPickerHost
import com.iosphone.app.ui.contacts.ContactsScreen
import com.iosphone.app.ui.theme.Ios

@Composable
fun ContactsShell() {
    val colors = Ios.colors
    val data = rememberAppData()
    val nav = remember { Nav() }
    var query by rememberSaveable { mutableStateOf("") }
    CompositionLocalProvider(LocalAppData provides data, LocalNav provides nav) {
        RequestPermissionsOnce()
        Box(Modifier.fillMaxSize().background(colors.background)) {
            val sheetBlur by animateFloatAsState(if (SimPicker.number != null) 40f else 0f, tween(240), label = "simBlur")
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    renderEffect = if (sheetBlur > 0.5f) BlurEffect(sheetBlur, sheetBlur, TileMode.Clamp) else null
                },
            ) { ContactsScreen(query) }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (SimPicker.number != null) 1f else 0f }.background(Color.Black.copy(alpha = 0.3f)))
            GlassSearchField(
                value = query,
                onChange = { query = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
            EdgeScrims(if (nav.stack.isNotEmpty() && !colors.dark) colors.grouped else colors.background)
            RouteLayer(nav)
            SimPickerHost()
        }
    }
}
