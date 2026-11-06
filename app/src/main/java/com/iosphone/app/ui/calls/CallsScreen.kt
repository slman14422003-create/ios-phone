package com.iosphone.app.ui.calls

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.CallEntry
import com.iosphone.app.data.CallLogRepository
import com.iosphone.app.data.Contact
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.DefaultDialer
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.GlassMenu
import com.iosphone.app.ui.common.GlassTextButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.MenuItem
import com.iosphone.app.ui.common.PermissionGate
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.launch

private class CallGroup(val first: CallEntry, val ids: List<Long>) {
    val count: Int get() = ids.size
}

private fun group(entries: List<CallEntry>): List<CallGroup> {
    val out = ArrayList<CallGroup>()
    var current: CallEntry? = null
    var ids = ArrayList<Long>()
    for (e in entries) {
        val c = current
        if (c != null && c.number == e.number && c.isMissed == e.isMissed && c.type == e.type) {
            ids.add(e.id)
        } else {
            if (c != null) out.add(CallGroup(c, ids))
            current = e
            ids = arrayListOf(e.id)
        }
    }
    current?.let { out.add(CallGroup(it, ids)) }
    return out
}

@Composable
fun CallsScreen() {
    PermissionGate(
        Manifest.permission.READ_CALL_LOG,
        stringResource(R.string.calllog_perm_title),
        stringResource(R.string.calllog_perm_body),
    ) { CallsContent() }
}

@Composable
private fun CallsContent() {
    val context = LocalContext.current
    val colors = Ios.colors
    val data = LocalAppData.current
    val nav = LocalNav.current
    val call = rememberCaller()
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val groups = remember(data.calls, filter) {
        group(
            data.calls.filter {
                when (filter) {
                    1 -> it.isMissed
                    2 -> it.isOutgoing
                    3 -> !it.isOutgoing && !it.isMissed
                    else -> true
                }
            },
        )
    }
    val favorites = remember(data.contacts) { data.contacts.filter { it.starred && it.phones.isNotEmpty() } }
    var isDefault by remember(data.version) { mutableStateOf(DefaultDialer.isDefault(context)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = DefaultDialer.isDefault(context)
    }
    val titles = listOf(R.string.calls_title, R.string.missed, R.string.outgoing, R.string.incoming)
    val callsTitle = stringResource(titles[filter])
    val allCallsLabel = stringResource(R.string.all_calls)
    val missedLabel = stringResource(R.string.missed)
    val outgoingLabel = stringResource(R.string.outgoing)
    val incomingLabel = stringResource(R.string.incoming)
    val voicemailLabel = stringResource(R.string.voicemail_title)

    LazyColumn(
        Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        item(key = "bar") {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTextButton(
                    stringResource(if (editing) R.string.done else R.string.edit),
                    onClick = { editing = !editing },
                    bold = editing,
                )
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Filled.Settings, onClick = { nav.push(Route.Settings) })
                Spacer(Modifier.width(10.dp))
                Box {
                    GlassIconButton(Icons.Filled.FilterList, onClick = { menu = true })
                    GlassMenu(
                        expanded = menu,
                        onDismiss = { menu = false },
                        items = listOf(
                            MenuItem(allCallsLabel, checked = filter == 0) { filter = 0 },
                            MenuItem(missedLabel, checked = filter == 1) { filter = 1 },
                            MenuItem(outgoingLabel, checked = filter == 2) { filter = 2 },
                            MenuItem(incomingLabel, checked = filter == 3) { filter = 3 },
                            MenuItem(voicemailLabel, icon = Icons.Filled.Voicemail) { nav.push(Route.Voicemail) },
                        ),
                    )
                }
            }
        }
        item(key = "title") {
            BasicText(
                callsTitle,
                style = IosType.largeTitle.copy(color = colors.label),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 10.dp),
            )
        }
        if (!isDefault) {
            item(key = "default") {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (colors.dark) colors.card else colors.grouped)
                        .clickable { roleLauncher.launch(DefaultDialer.requestIntent(context)) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(stringResource(R.string.default_title), style = IosType.headline.copy(color = colors.label))
                        BasicText(
                            stringResource(R.string.default_body),
                            style = IosType.footnote.copy(color = colors.secondaryLabel),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    BasicText(
                        stringResource(R.string.set_default),
                        style = IosType.body.copy(color = colors.tint, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
        if (filter == 0 && !editing && favorites.isNotEmpty()) {
            item(key = "favorites") {
                Column(Modifier.padding(bottom = 8.dp)) {
                    BasicText(
                        stringResource(R.string.favorites_title),
                        style = IosType.footnote.copy(color = colors.secondaryLabel, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(favorites, key = { it.id }) { c -> FavoriteChip(c) { call(c.phones.first().number) } }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (data.loaded && groups.isEmpty()) {
            item(key = "empty") {
                BasicText(
                    stringResource(R.string.no_recents),
                    style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                )
            }
        }
        items(groups, key = { it.first.id }, contentType = { "call" }) { g ->
            val contact = data.contactFor(g.first.number)
            CallRow(
                g = g,
                contact = contact,
                editing = editing,
                onClick = { call(g.first.number) },
                onInfo = {
                    if (contact != null) nav.push(Route.ContactDetail(contact.id))
                    else nav.push(Route.CallInfo(g.first.number))
                },
                onDelete = {
                    scope.launch {
                        CallLogRepository.delete(context, g.ids)
                        data.invalidate()
                    }
                },
            )
        }
    }
}

@Composable
private fun FavoriteChip(c: Contact, onClick: () -> Unit) {
    val colors = Ios.colors
    Column(
        Modifier.width(72.dp).pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(c.name, c.thumbUri, 64.dp)
        Spacer(Modifier.height(6.dp))
        BasicText(
            c.name.split(' ').first(),
            style = IosType.footnote.copy(color = colors.label, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CallRow(
    g: CallGroup,
    contact: Contact?,
    editing: Boolean,
    onClick: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Ios.colors
    val e = g.first
    val title = (contact?.name ?: e.name ?: PhoneFormat.format(e.number).ifEmpty { "Unknown" }) +
        if (g.count > 1) " (" + g.count + ")" else ""
    val label = contact?.phones?.firstOrNull { PhoneFormat.key(it.number) == PhoneFormat.key(e.number) }?.label ?: "mobile"
    val icon = when {
        e.isMissed -> Icons.AutoMirrored.Filled.CallMissed
        e.isOutgoing -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    Column(Modifier.fillMaxWidth().clickable(enabled = !editing, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(colors.red).clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(Icons.Filled.Remove, null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                BasicText(
                    title,
                    style = IosType.body.copy(color = if (e.isMissed) colors.red else colors.label, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(icon, null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
                    Spacer(Modifier.width(4.dp))
                    BasicText(label, style = IosType.callout.copy(color = colors.secondaryLabel), maxLines = 1)
                }
            }
            BasicText(Formats.callDate(e.date), style = IosType.callout.copy(color = colors.secondaryLabel))
            Spacer(Modifier.width(10.dp))
            Image(
                Icons.Outlined.Info,
                null,
                Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onInfo),
                colorFilter = ColorFilter.tint(colors.tint),
            )
        }
        Hairline(if (editing) 52.dp else 16.dp)
    }
}
