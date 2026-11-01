package com.iosphone.app.ui.favorites

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.data.ContactsRepository
import com.iosphone.app.ui.common.PermissionGate
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType

@Composable
fun FavoritesScreen() {
    PermissionGate(
        Manifest.permission.READ_CONTACTS,
        stringResource(R.string.contacts_perm_title),
        stringResource(R.string.contacts_perm_body),
    ) { FavoritesContent() }
}

@Composable
private fun FavoritesContent() {
    val context = LocalContext.current
    val colors = Ios.colors
    val call = rememberCaller()
    val loaded by produceState<List<Contact>?>(null) { value = ContactsRepository.load(context) }
    val all = loaded
    val favs = remember(all) { all.orEmpty().filter { it.starred && it.phones.isNotEmpty() } }

    LazyColumn(Modifier.fillMaxSize().background(colors.background)) {
        item(key = "title") {
            BasicText(
                stringResource(R.string.favorites_title),
                style = IosType.largeTitle.copy(color = colors.label),
                modifier = Modifier.padding(start = 16.dp, top = 52.dp, bottom = 8.dp),
            )
        }
        if (all != null && favs.isEmpty()) {
            item(key = "empty") {
                BasicText(
                    stringResource(R.string.no_favorites),
                    style = IosType.body.copy(color = colors.secondaryLabel),
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        items(favs, key = { it.id }) { c ->
            val phone = c.phones.first()
            Column(Modifier.fillMaxWidth().clickable { call(phone.number) }) {
                Column(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    BasicText(c.name, style = IosType.body.copy(color = colors.label), maxLines = 1)
                    BasicText(phone.label, style = IosType.footnote.copy(color = colors.secondaryLabel))
                }
                Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
            }
        }
    }
}
