package com.iosphone.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.CallEntry
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.domain.ContactSearch
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat

@Composable
fun SearchScreen(query: String) {
    val colors = Ios.colors
    val data = LocalAppData.current
    val nav = LocalNav.current
    val call = rememberCaller()
    val q = query.trim()
    val digits = q.filter { it.isDigit() || it == '+' }
    val contacts = remember(data.contacts, q) { if (q.isEmpty()) emptyList() else ContactSearch.filter(data.contacts, q) }
    val recents = remember(data.calls, data.contacts, q) {
        if (q.isEmpty()) {
            emptyList<CallEntry>()
        } else {
            val seen = HashSet<String>()
            data.calls.filter { e ->
                val name = data.contactFor(e.number)?.name ?: e.name.orEmpty()
                val hit = name.contains(q, ignoreCase = true) ||
                    (digits.length >= 2 && e.number.filter { it.isDigit() }.contains(digits.filter { it.isDigit() }))
                hit && seen.add(PhoneFormat.key(e.number))
            }.take(15)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 160.dp),
    ) {
        if (q.isEmpty()) {
            item(key = "hint") {
                BasicText(
                    stringResource(R.string.search_hint),
                    style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                )
            }
        }
        if (digits.length >= 3) {
            item(key = "dial") {
                Column(Modifier.fillMaxWidth().clickable { call(digits) }) {
                    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            stringResource(R.string.call_number, PhoneFormat.format(digits)),
                            style = IosType.body.copy(color = colors.tint),
                        )
                    }
                    Hairline()
                }
            }
        }
        if (contacts.isNotEmpty()) {
            item(key = "ch") { SectionTitle(stringResource(R.string.contacts_title)) }
            items(contacts, key = { "c" + it.id }) { c ->
                Column(Modifier.fillMaxWidth().clickable { nav.push(Route.ContactDetail(c.id)) }) {
                    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.name, c.thumbUri, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            BasicText(c.name, style = IosType.body.copy(color = colors.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            c.phones.firstOrNull()?.let {
                                BasicText(PhoneFormat.format(it.number), style = IosType.footnote.copy(color = colors.secondaryLabel), maxLines = 1)
                            }
                        }
                    }
                    Hairline(66.dp)
                }
            }
        }
        if (recents.isNotEmpty()) {
            item(key = "rh") { SectionTitle(stringResource(R.string.calls_title)) }
            items(recents, key = { "r" + it.id }) { e ->
                val contact = data.contactFor(e.number)
                Column(Modifier.fillMaxWidth().clickable { call(e.number) }) {
                    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                contact?.name ?: e.name ?: PhoneFormat.format(e.number),
                                style = IosType.body.copy(color = if (e.isMissed) colors.red else colors.label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            BasicText(PhoneFormat.format(e.number), style = IosType.footnote.copy(color = colors.secondaryLabel), maxLines = 1)
                        }
                        BasicText(Formats.callDate(e.date), style = IosType.callout.copy(color = colors.secondaryLabel))
                    }
                    Hairline()
                }
            }
        }
        if (q.isNotEmpty() && contacts.isEmpty() && recents.isEmpty() && digits.length < 3) {
            item(key = "none") {
                BasicText(
                    stringResource(R.string.no_results),
                    style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = Ios.colors
    BasicText(
        text,
        style = IosType.footnote.copy(color = colors.secondaryLabel, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
    )
}
