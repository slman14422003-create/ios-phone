package com.iosphone.app.ui.contacts

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.domain.ContactSearch
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.PermissionGate
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val sectionComparator = Comparator<String> { a, b ->
    when {
        a == "#" && b != "#" -> 1
        b == "#" && a != "#" -> -1
        else -> a.compareTo(b)
    }
}

private fun sectionOf(name: String): String {
    val c = name.trim().firstOrNull()?.uppercaseChar()
    return if (c != null && c.isLetter()) c.toString() else "#"
}

@Composable
fun ContactsScreen(query: String = "") {
    PermissionGate(
        Manifest.permission.READ_CONTACTS,
        stringResource(R.string.contacts_perm_title),
        stringResource(R.string.contacts_perm_body),
    ) { ContactList(query) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactList(query: String) {
    val colors = Ios.colors
    val data = LocalAppData.current
    val nav = LocalNav.current
    val all = data.contacts
    val filtered = remember(all, query) { ContactSearch.filter(all, query) }
    val sections = remember(filtered) {
        if (query.isBlank()) filtered.groupBy { sectionOf(it.name) }.toSortedMap(sectionComparator)
        else linkedMapOf("" to filtered)
    }
    val indexMap = remember(sections) {
        val map = LinkedHashMap<String, Int>()
        var i = 1
        for ((letter, people) in sections) {
            map[letter] = i
            i += (if (letter.isEmpty()) 0 else 1) + people.size
        }
        map
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 150.dp),
        ) {
            item(key = "top") {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Spacer(Modifier.weight(1f))
                        GlassIconButton(Icons.Filled.Add, onClick = { nav.push(Route.Editor(null, null)) })
                    }
                    BasicText(
                        stringResource(R.string.contacts_title),
                        style = IosType.largeTitle.copy(color = colors.label),
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 8.dp),
                    )
                }
            }
            for ((letter, people) in sections) {
                if (letter.isNotEmpty()) {
                    stickyHeader(key = "h_$letter") { SectionHeader(letter) }
                }
                items(people, key = { it.id }) { c -> ContactRow(c) { nav.push(Route.ContactDetail(c.id)) } }
            }
            if (filtered.isEmpty() && data.loaded) {
                item(key = "empty") {
                    BasicText(
                        stringResource(R.string.no_results),
                        style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                    )
                }
            }
        }
        if (query.isBlank() && sections.size > 1) {
            SideIndex(
                letters = sections.keys.toList(),
                onSelect = { l -> indexMap[l]?.let { scope.launch { listState.scrollToItem(it) } } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun SectionHeader(letter: String) {
    val colors = Ios.colors
    Box(
        Modifier.fillMaxWidth().height(30.dp).background(colors.background).padding(start = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            letter,
            style = IosType.body.copy(color = colors.secondaryLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
fun ContactRow(c: Contact, onClick: () -> Unit) {
    val colors = Ios.colors
    val text = remember(c.name, c.given, c.family) {
        buildAnnotatedString {
            if (c.given.isNotEmpty() && c.family.isNotEmpty()) {
                append(c.given + " ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(c.family) }
            } else {
                val parts = c.name.trim().split(' ').filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    append(parts.dropLast(1).joinToString(" ") + " ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(parts.last()) }
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(c.name) }
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(c.name, c.thumbUri, 38.dp)
            Spacer(Modifier.width(12.dp))
            BasicText(
                text,
                style = IosType.body.copy(color = colors.label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Hairline(66.dp)
    }
}

@Composable
private fun SideIndex(letters: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = Ios.colors
    val view = LocalView.current
    var heightPx by remember { mutableIntStateOf(1) }
    var current by remember { mutableStateOf<String?>(null) }
    var fingerY by remember { mutableFloatStateOf(0f) }

    fun pick(y: Float) {
        fingerY = y
        val idx = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[idx]
        if (letter != current) {
            current = letter
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            onSelect(letter)
        }
    }

    Box(modifier) {
        Column(
            Modifier
                .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
                .pointerInput(letters) { detectTapGestures(onTap = { pick(it.y) }) }
                .pointerInput(letters) {
                    detectDragGestures(
                        onDragStart = { pick(it.y) },
                        onDragEnd = { current = null },
                        onDragCancel = { current = null },
                        onDrag = { change, _ ->
                            change.consume()
                            pick(change.position.y)
                        },
                    )
                }
                .padding(horizontal = 6.dp),
        ) {
            letters.forEach { l ->
                Box(Modifier.height(15.dp).width(16.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        l,
                        style = IosType.footnote.copy(
                            color = colors.tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
        current?.let { letter ->
            Box(
                Modifier
                    .offset { IntOffset((-64).dp.roundToPx(), (fingerY - 22.dp.toPx()).roundToInt()) }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.selection),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(letter, style = IosType.title3.copy(color = colors.label, fontSize = 24.sp))
            }
        }
    }
}
