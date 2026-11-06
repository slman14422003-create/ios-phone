package com.iosphone.app.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.rememberAppData
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.calls.CallsScreen
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.GlassSearchField
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Nav
import com.iosphone.app.ui.common.RequestPermissionsOnce
import com.iosphone.app.ui.common.RouteLayer
import com.iosphone.app.ui.common.glass
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.contacts.ContactsScreen
import com.iosphone.app.ui.keypad.KeypadScreen
import com.iosphone.app.ui.search.SearchScreen
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

private class TabSpec(val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabSpec(R.string.tab_calls, Icons.Filled.AccessTime),
    TabSpec(R.string.tab_contacts, Icons.Filled.AccountCircle),
    TabSpec(R.string.tab_keypad, Icons.Filled.Dialpad),
)

@Composable
fun PhoneShell(dialNumber: String?) {
    val colors = Ios.colors
    val data = rememberAppData()
    val nav = remember { Nav() }
    var tab by rememberSaveable { mutableIntStateOf(if (dialNumber != null) 2 else 0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    CompositionLocalProvider(LocalAppData provides data, LocalNav provides nav) {
        RequestPermissionsOnce()
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Crossfade(targetState = if (searching) 3 else tab, animationSpec = tween(220), label = "tabs") { page ->
                when (page) {
                    0 -> CallsScreen()
                    1 -> ContactsScreen()
                    2 -> KeypadScreen(dialNumber)
                    else -> SearchScreen(query)
                }
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
                BottomBar(
                    tab = tab,
                    searching = searching,
                    query = query,
                    onQuery = { query = it },
                    onTab = {
                        tab = it
                        searching = false
                        query = ""
                    },
                    onSearching = {
                        searching = it
                        if (!it) query = ""
                    },
                )
            }
            RouteLayer(nav)
        }
    }
}

@Composable
private fun BottomBar(
    tab: Int,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onTab: (Int) -> Unit,
    onSearching: (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = searching,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        label = "bar",
    ) { isSearching ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSearching) {
                GlassSearchField(query, onQuery, Modifier.weight(1f), autoFocus = true)
                GlassIconButton(Icons.Filled.Close, onClick = { onSearching(false) }, size = 62.dp, iconSize = 24.dp)
            } else {
                TabPill(tab, onTab, Modifier.weight(1f))
                GlassIconButton(Icons.Filled.Search, onClick = { onSearching(true) }, size = 62.dp, iconSize = 26.dp)
            }
        }
    }
}

@Composable
private fun TabPill(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ios.colors
    BoxWithConstraints(modifier.height(62.dp).glass(CircleShape)) {
        val tabWidth = maxWidth / tabs.size
        val x by animateDpAsState(
            targetValue = tabWidth * selected,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "pill",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(5.dp)
                .clip(CircleShape)
                .background(colors.selection),
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, spec ->
                val active = index == selected
                val tint = if (active) colors.tint else colors.label
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable(pressedScale = 0.94f) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(spec.icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(tint))
                    BasicText(stringResource(spec.label), style = IosType.tabLabel.copy(color = tint))
                }
            }
        }
    }
}
