package com.iosphone.app.ui.recents

import android.Manifest
import android.provider.CallLog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.CallEntry
import com.iosphone.app.data.CallLogRepository
import com.iosphone.app.ui.common.PermissionGate
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat

private data class CallGroup(val first: CallEntry, val count: Int)

private fun group(entries: List<CallEntry>): List<CallGroup> {
    val out = ArrayList<CallGroup>()
    for (e in entries) {
        val last = out.lastOrNull()
        if (last != null && last.first.number == e.number && last.first.isMissed == e.isMissed) {
            out[out.lastIndex] = last.copy(count = last.count + 1)
        } else {
            out.add(CallGroup(e, 1))
        }
    }
    return out
}

@Composable
fun RecentsScreen() {
    PermissionGate(
        Manifest.permission.READ_CALL_LOG,
        stringResource(R.string.calllog_perm_title),
        stringResource(R.string.calllog_perm_body),
    ) { RecentsContent() }
}

@Composable
private fun RecentsContent() {
    val context = LocalContext.current
    val colors = Ios.colors
    val call = rememberCaller()
    val entries by produceState<List<CallEntry>?>(null) { value = CallLogRepository.load(context) }
    var missedOnly by rememberSaveable { mutableStateOf(false) }
    val list = entries
    val groups = remember(list, missedOnly) {
        group(list.orEmpty().filter { !missedOnly || it.isMissed })
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Segmented(missedOnly) { missedOnly = it }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "title") {
                BasicText(
                    stringResource(R.string.recents_title),
                    style = IosType.largeTitle.copy(color = colors.label),
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }
            if (list != null && groups.isEmpty()) {
                item(key = "empty") {
                    BasicText(
                        stringResource(R.string.no_recents),
                        style = IosType.body.copy(color = colors.secondaryLabel),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(groups, key = { it.first.id }) { g -> RecentRow(g) { call(g.first.number) } }
        }
    }
}

@Composable
private fun RecentRow(g: CallGroup, onClick: () -> Unit) {
    val colors = Ios.colors
    val e = g.first
    val title = (e.name ?: PhoneFormat.format(e.number).ifEmpty { "?" }) +
        if (g.count > 1) " (" + g.count + ")" else ""
    val icon = when {
        e.isMissed -> Icons.AutoMirrored.Filled.CallMissed
        e.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    title,
                    style = IosType.body.copy(color = if (e.isMissed) colors.red else colors.label),
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(icon, null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        PhoneFormat.format(e.number),
                        style = IosType.footnote.copy(color = colors.secondaryLabel),
                        maxLines = 1,
                    )
                }
            }
            BasicText(
                Formats.callDate(e.date),
                style = IosType.body.copy(color = colors.secondaryLabel, fontSize = 15.sp),
            )
        }
        Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
    }
}

@Composable
private fun Segmented(missedOnly: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Ios.colors
    val thumb = if (colors.dark) colors.fillPressed else colors.background
    Row(
        Modifier.width(180.dp).height(32.dp).clip(RoundedCornerShape(9.dp)).background(colors.searchFill).padding(2.dp),
    ) {
        listOf(false to R.string.all, true to R.string.missed).forEach { (value, label) ->
            val selected = missedOnly == value
            Box(
                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(7.dp))
                    .background(if (selected) thumb else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(label),
                    style = IosType.footnote.copy(
                        color = colors.label,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}
