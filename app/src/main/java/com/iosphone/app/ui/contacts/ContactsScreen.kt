package com.iosphone.app.ui.contacts

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.iosphone.app.ui.common.glass
import kotlin.math.exp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

private const val ArabicOrder = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

private fun normalizeLetter(c: Char): Char = when (c) {
    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
    'ى', 'ئ' -> 'ي'
    'ة' -> 'ه'
    'ؤ' -> 'و'
    else -> c
}

private fun sectionRank(section: String): Int {
    if (section == "#") return 100000
    val c = section[0]
    val arabic = ArabicOrder.indexOf(c)
    return when {
        arabic >= 0 -> arabic
        c.code < 0x250 -> 1000 + c.code
        else -> 5000 + c.code
    }
}

private val sectionComparator = Comparator<String> { a, b -> sectionRank(a).compareTo(sectionRank(b)) }

private fun sectionOf(name: String): String {
    val first = name.trim().firstOrNull() ?: return "#"
    if (!first.isLetter()) return "#"
    return normalizeLetter(first).uppercaseChar().toString()
}

@Composable
fun ContactsScreen(query: String = "") {
    PermissionGate(
        Manifest.permission.READ_CONTACTS,
        stringResource(R.string.contacts_perm_title),
        stringResource(R.string.contacts_perm_body),
    ) { ContactList(query) }
}

private val IndexWidth = 30.dp

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
    val showIndex = query.isBlank() && sections.size > 1
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var lastLetter by remember { mutableStateOf("") }
    LaunchedEffect(activeLetter) { activeLetter?.let { lastLetter = it } }
    val blurRadius by animateDpAsState(
        targetValue = if (activeLetter != null) 22.dp else 0.dp,
        animationSpec = tween(260),
        label = "indexBlur",
    )
    val dim by animateFloatAsState(
        targetValue = if (activeLetter != null) 0.32f else 0f,
        animationSpec = tween(260),
        label = "indexDim",
    )

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded) else Modifier),
            contentPadding = PaddingValues(bottom = 150.dp),
        ) {
            item(key = "top", contentType = "top") {
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
                    stickyHeader(key = "h_$letter", contentType = "header") { SectionHeader(letter) }
                }
                items(people, key = { it.id }, contentType = { "contact" }) { c ->
                    ContactRow(c, endInset = if (showIndex) IndexWidth - 4.dp else 0.dp) { nav.push(Route.ContactDetail(c.id)) }
                }
            }
            if (filtered.isEmpty() && data.loaded) {
                item(key = "empty", contentType = "empty") {
                    BasicText(
                        stringResource(R.string.no_results),
                        style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                    )
                }
            }
        }
        if (dim > 0.001f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
        AnimatedVisibility(
            visible = activeLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.6f),
            exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f),
        ) {
            BasicText(
                lastLetter,
                style = IosType.largeTitle.copy(color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Bold),
            )
        }
        if (showIndex) {
            SideIndex(
                letters = sections.keys.toList(),
                onSelect = { l -> indexMap[l]?.let { listState.requestScrollToItem(it) } },
                onActive = { activeLetter = it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .padding(top = 112.dp, bottom = 150.dp),
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
fun ContactRow(c: Contact, endInset: Dp = 0.dp, onClick: () -> Unit) {
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
        Hairline(66.dp, endInset)
    }
}

@Composable
private fun SideIndex(
    letters: List<String>,
    onSelect: (String) -> Unit,
    onActive: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Ios.colors
    val view = LocalView.current
    val density = LocalDensity.current
    val currentLetters by rememberUpdatedState(letters)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentActive by rememberUpdatedState(onActive)
    var fingerY by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }
    var lastIndex by remember { mutableStateOf(-1) }
    val activation = animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "indexWave",
    )

    BoxWithConstraints(modifier.fillMaxHeight().width(IndexWidth)) {
        val availPx = with(density) { maxHeight.toPx() }
        val minSlot = with(density) { 13.dp.toPx() }
        val maxSlot = with(density) { 19.dp.toPx() }
        val slotPx = (availPx / letters.size).coerceIn(minSlot, maxSlot)
        val topPx = ((availPx - slotPx * letters.size) / 2f).coerceAtLeast(0f)
        val waveShift = with(density) { 34.dp.toPx() }
        val slotDp = with(density) { slotPx.toDp() }

        fun pick(y: Float) {
            fingerY = y
            val list = currentLetters
            val idx = ((y - topPx) / slotPx).toInt().coerceIn(0, list.size - 1)
            if (idx != lastIndex) {
                lastIndex = idx
                val tick = if (android.os.Build.VERSION.SDK_INT >= 34) {
                    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
                view.performHapticFeedback(tick)
                currentActive(list[idx])
                currentSelect(list[idx])
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(topPx, slotPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        active = true
                        lastIndex = -1
                        pick(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            pick(change.position.y)
                        }
                        active = false
                        lastIndex = -1
                        currentActive(null)
                    }
                },
        ) {
            Column(Modifier.padding(top = with(density) { topPx.toDp() })) {
                letters.forEachIndexed { i, letter ->
                    Box(
                        Modifier
                            .height(slotDp)
                            .width(IndexWidth)
                            .graphicsLayer {
                                val a = activation.value
                                if (a > 0.001f) {
                                    val center = topPx + slotPx * (i + 0.5f)
                                    val d = (center - fingerY) / slotPx
                                    val bump = exp(-(d * d) / 5f) * a
                                    translationX = -bump * waveShift
                                    scaleX = 1f + 0.7f * bump
                                    scaleY = 1f + 0.7f * bump
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            letter,
                            style = IosType.caption.copy(
                                color = colors.tint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
