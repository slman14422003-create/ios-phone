package com.iosphone.app.ui.contacts

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.data.ContactsRepository
import com.iosphone.app.domain.ContactSearch
import com.iosphone.app.ui.common.PermissionGate
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
fun ContactsScreen() {
    PermissionGate(
        Manifest.permission.READ_CONTACTS,
        stringResource(R.string.contacts_perm_title),
        stringResource(R.string.contacts_perm_body),
    ) { ContactsContent() }
}

@Composable
private fun ContactsContent() {
    val context = LocalContext.current
    val loaded by produceState<List<Contact>?>(null) { value = ContactsRepository.load(context) }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val all = loaded
    val open = all?.firstOrNull { it.id == openId }
    when {
        all == null -> Box(Modifier.fillMaxSize().background(Ios.colors.background))
        open != null -> {
            BackHandler { openId = null }
            ContactDetailScreen(open) { openId = null }
        }
        else -> ContactList(all) { openId = it.id }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactList(all: List<Contact>, onOpen: (Contact) -> Unit) {
    val colors = Ios.colors
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(all, query) { ContactSearch.filter(all, query) }
    val sections = remember(filtered) { filtered.groupBy { sectionOf(it.name) }.toSortedMap(sectionComparator) }
    val indexMap = remember(sections) {
        val map = LinkedHashMap<String, Int>()
        var i = 2
        for ((letter, people) in sections) {
            map[letter] = i
            i += 1 + people.size
        }
        map
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "title") {
                BasicText(
                    stringResource(R.string.contacts_title),
                    style = IosType.largeTitle.copy(color = colors.label),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            item(key = "search") { SearchField(query) { query = it } }
            for ((letter, people) in sections) {
                stickyHeader(key = "h_$letter") { SectionHeader(letter) }
                items(people, key = { it.id }) { c -> ContactRow(c) { onOpen(c) } }
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    BasicText(
                        stringResource(R.string.no_results),
                        style = IosType.body.copy(color = colors.secondaryLabel),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
        }
        if (query.isBlank() && sections.isNotEmpty()) {
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
        Modifier.fillMaxWidth().height(28.dp).background(colors.secondaryBackground).padding(start = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            letter,
            style = IosType.body.copy(color = colors.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.searchFill)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(Icons.Filled.Search, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                BasicText(
                    stringResource(R.string.search),
                    style = IosType.body.copy(color = colors.secondaryLabel),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = IosType.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.tint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Image(
                Icons.Filled.Cancel,
                null,
                Modifier.size(18.dp).clickable { onChange("") },
                colorFilter = ColorFilter.tint(colors.secondaryLabel),
            )
        }
    }
}

@Composable
private fun ContactRow(c: Contact, onClick: () -> Unit) {
    val colors = Ios.colors
    val text = remember(c.name) {
        val parts = c.name.trim().split(' ').filter { it.isNotEmpty() }
        buildAnnotatedString {
            if (parts.size > 1) {
                append(parts.dropLast(1).joinToString(" ") + " ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(parts.last()) }
            } else {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(c.name) }
            }
        }
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).padding(start = 16.dp, end = 32.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(text, style = IosType.body.copy(color = colors.label), maxLines = 1)
        }
        Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(colors.separator))
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
                    .background(colors.fillPressed),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(letter, style = IosType.title3.copy(color = colors.label, fontSize = 24.sp))
            }
        }
    }
}
