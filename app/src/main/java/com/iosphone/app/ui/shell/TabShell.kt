package com.iosphone.app.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.ui.contacts.ContactsScreen
import com.iosphone.app.ui.favorites.FavoritesScreen
import com.iosphone.app.ui.keypad.KeypadScreen
import com.iosphone.app.ui.recents.RecentsScreen
import com.iosphone.app.ui.voicemail.VoicemailScreen
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

private data class Tab(val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(R.string.tab_favorites, Icons.Filled.Star),
    Tab(R.string.tab_recents, Icons.Filled.AccessTime),
    Tab(R.string.tab_contacts, Icons.Filled.Person),
    Tab(R.string.tab_keypad, Icons.Filled.Dialpad),
    Tab(R.string.tab_voicemail, Icons.Filled.Voicemail),
)

@Composable
fun TabShell() {
    val colors = Ios.colors
    var selected by rememberSaveable { mutableIntStateOf(3) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            when (selected) {
                0 -> FavoritesScreen()
                1 -> RecentsScreen()
                2 -> ContactsScreen()
                3 -> KeypadScreen()
                else -> VoicemailScreen()
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.barSeparator))
        Row(
            Modifier.fillMaxWidth().background(colors.bar).navigationBarsPadding().height(49.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = index == selected
                val tint = if (active) colors.tint else colors.secondaryLabel
                Column(
                    Modifier.weight(1f).fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { selected = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Image(tab.icon, null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(tint))
                    BasicText(stringResource(tab.label), style = IosType.tabLabel.copy(color = tint))
                }
            }
        }
    }
}
