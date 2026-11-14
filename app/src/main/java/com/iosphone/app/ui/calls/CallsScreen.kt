package com.iosphone.app.ui.calls

import com.iosphone.app.ui.common.labelText
import com.iosphone.app.telecom.ContactsLink
import com.iosphone.app.ui.theme.ltr
import com.iosphone.app.ui.theme.autoDir
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.iosphone.app.ui.common.IosIcons
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
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.iosphone.app.domain.CallIntel
import com.iosphone.app.telecom.Messaging
import com.iosphone.app.ui.common.QuickMenu
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.iosphone.app.data.AppData
import com.iosphone.app.telecom.MissedCalls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

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
    LaunchedEffect(data.pendingFilter) {
        if (data.pendingFilter >= 0) {
            filter = data.pendingFilter
            data.pendingFilter = -1
        }
    }
    var menu by remember { mutableStateOf(false) }
    var openId by remember { mutableLongStateOf(-1L) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openId = -1L }
    LaunchedEffect(editing, filter) { openId = -1L }
    val lifecycleOwner = LocalLifecycleOwner.current
    val unreadMissed = data.missedUnread
    LaunchedEffect(filter, unreadMissed, data.resumeCount) {
        if (filter == 1 && unreadMissed > 0 && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            withContext(Dispatchers.IO) { MissedCalls.clear(context) }
            AppData.shared.invalidateCalls()
        }
    }
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
    val view = LocalView.current
    val suggested = remember(data.calls, data.contacts, favorites) {
        val favIds = favorites.map { it.id }.toHashSet()
        val seen = HashSet<Long>()
        val out = ArrayList<Pair<Contact, String>>()
        for (pick in CallIntel.suggestions(data.calls, limit = 12)) {
            val c = data.contactFor(pick.number) ?: continue
            if (c.id in favIds || !seen.add(c.id)) continue
            out.add(c to pick.number)
            if (out.size >= 8) break
        }
        out
    }
    val unreturned = remember(data.calls) { CallIntel.unreturnedMissed(data.calls) }
    val spamKeys = remember(data.calls, data.contacts) { CallIntel.spamNumbers(data.calls) { data.contactFor(it) != null } }
    val suggestedLabel = stringResource(R.string.suggested_title)
    val notReturnedLabel = stringResource(R.string.not_returned)
    val spamLabel = stringResource(R.string.possible_spam)
    val callActionLabel = stringResource(R.string.call_action)
    val messageActionLabel = stringResource(R.string.send_message)
    val rowPlacement = remember {
        spring<IntOffset>(dampingRatio = 1f, stiffness = 700f, visibilityThreshold = IntOffset.VisibilityThreshold)
    }
    var isDefault by remember(data.resumeCount) { mutableStateOf(DefaultDialer.isDefault(context)) }
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
        state = listState,
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
                            MenuItem(allCallsLabel, icon = IosIcons.AllCalls, checked = filter == 0) { filter = 0 },
                            MenuItem(missedLabel, icon = IosIcons.Missed, checked = filter == 1) { filter = 1 },
                            MenuItem(outgoingLabel, icon = IosIcons.Outgoing, checked = filter == 2) { filter = 2 },
                            MenuItem(incomingLabel, icon = IosIcons.Incoming, checked = filter == 3) { filter = 3 },
                            MenuItem(voicemailLabel, icon = IosIcons.Voicemail) { nav.push(Route.Voicemail) },
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
        if (filter == 0 && !editing && suggested.isNotEmpty()) {
            item(key = "suggested") {
                Column(Modifier.padding(bottom = 8.dp)) {
                    BasicText(
                        suggestedLabel,
                        style = IosType.footnote.copy(color = colors.secondaryLabel, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(suggested, key = { it.first.id }) { (c, number) -> FavoriteChip(c) { call(number) } }
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
            var menu by remember { mutableStateOf(false) }
            val hint = when {
                g.first.id in unreturned -> notReturnedLabel
                contact == null && g.first.name == null && PhoneFormat.key(g.first.number) in spamKeys -> spamLabel
                else -> null
            }
            Box(Modifier.animateItem(placementSpec = rowPlacement)) {
                SwipeRow(
                    rowKey = g.first.id,
                    openKey = openId,
                    enabled = !editing,
                    canContact = g.first.number.isNotBlank(),
                    onOpened = { openId = g.first.id },
                    onClosed = { if (openId == g.first.id) openId = -1L },
                    onCall = { call(g.first.number) },
                    onMessage = { Messaging.open(context, g.first.number) },
                    onDelete = {
                        scope.launch {
                            CallLogRepository.delete(context, g.ids)
                            AppData.shared.invalidateCalls()
                        }
                    },
                ) {
                    CallRow(
                        g = g,
                        contact = contact,
                        editing = editing,
                        hint = hint,
                        hintIsSpam = hint == spamLabel,
                        onClick = { call(g.first.number) },
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            menu = true
                        },
                        onInfo = {
                            if (contact != null) ContactsLink.openContact(context, contact.id)
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
                QuickMenu(
                    expanded = menu,
                    onDismiss = { menu = false },
                    contact = contact,
                    number = g.first.number,
                    extra = listOf(
                        MenuItem(callActionLabel, icon = Icons.Filled.Call) { call(g.first.number) },
                        MenuItem(messageActionLabel, icon = Icons.Filled.ChatBubble) { Messaging.open(context, g.first.number) },
                    ),
                )
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallRow(
    g: CallGroup,
    contact: Contact?,
    editing: Boolean,
    hint: String?,
    hintIsSpam: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Ios.colors
    val e = g.first
    val unknownLabel = stringResource(R.string.unknown)
    val isNumberTitle = contact?.name == null && e.name == null
    val title = (contact?.name ?: e.name ?: PhoneFormat.format(e.number).ifEmpty { unknownLabel }) +
        if (g.count > 1) " (" + g.count + ")" else ""
    val label = labelText(contact?.phones?.firstOrNull { PhoneFormat.key(it.number) == PhoneFormat.key(e.number) }?.label ?: "mobile")
    val icon = when {
        e.isMissed -> Icons.AutoMirrored.Filled.CallMissed
        e.isOutgoing -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallReceived
    }
    Column(Modifier.fillMaxWidth().combinedClickable(enabled = !editing, onClick = onClick, onLongClick = onLongClick)) {
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
                    style = IosType.body.copy(color = if (e.isMissed) colors.red else colors.label, fontWeight = FontWeight.Medium)
                        .let { if (isNumberTitle) it.ltr() else it.autoDir() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(icon, null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(colors.secondaryLabel))
                    Spacer(Modifier.width(4.dp))
                    BasicText(label, style = IosType.callout.copy(color = colors.secondaryLabel), maxLines = 1)
                    if (hint != null) {
                        BasicText(
                            "  " + hint,
                            style = IosType.callout.copy(color = if (hintIsSpam) colors.red else colors.secondaryLabel),
                            maxLines = 1,
                        )
                    }
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

private enum class SwipeZone { Closed, Open, Call }

@Composable
private fun SwipeRow(
    rowKey: Long,
    openKey: Long,
    enabled: Boolean,
    canContact: Boolean,
    onOpened: () -> Unit,
    onClosed: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = Ios.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    var x by remember { mutableFloatStateOf(0f) }
    var rowWidth by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current
    val buttonWidthPx = with(density) { 80.dp.toPx() }
    val actionsWidthPx = if (canContact) buttonWidthPx * 2f else buttonWidthPx
    val callThresholdPx = maxOf(with(density) { 110.dp.toPx() }, rowWidth * 0.32f)
    val openThresholdPx = actionsWidthPx * 0.35f
    val currentOnOpened by rememberUpdatedState(onOpened)
    val currentOnClosed by rememberUpdatedState(onClosed)
    val currentOnCall by rememberUpdatedState(onCall)
    val spec = remember { spring<Float>(dampingRatio = 0.8f, stiffness = 420f) }

    fun settleTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch { animate(x, target, animationSpec = spec) { v, _ -> x = v } }
    }

    fun zone(v: Float): SwipeZone = when {
        v >= callThresholdPx -> SwipeZone.Call
        v <= -openThresholdPx -> SwipeZone.Open
        else -> SwipeZone.Closed
    }

    LaunchedEffect(openKey, enabled) {
        if ((openKey != rowKey || !enabled) && x != 0f) settleTo(0f)
    }

    val isOpen by remember(openKey, rowKey) { derivedStateOf { x < -1f && openKey == rowKey } }

    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { rowWidth = it.width.toFloat() }
            .pointerInput(enabled, canContact, actionsWidthPx, callThresholdPx, sign) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { settleJob?.cancel() },
                    onDragEnd = {
                        when (zone(x)) {
                            SwipeZone.Call -> {
                                currentOnCall()
                                settleTo(0f)
                                currentOnClosed()
                            }
                            SwipeZone.Open -> {
                                settleTo(-actionsWidthPx)
                                currentOnOpened()
                            }
                            SwipeZone.Closed -> {
                                settleTo(0f)
                                currentOnClosed()
                            }
                        }
                    },
                    onDragCancel = {
                        settleTo(0f)
                        currentOnClosed()
                    },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        val before = x
                        val maxRight = if (canContact) rowWidth * 0.6f else 0f
                        val minLeft = -(actionsWidthPx * 1.2f)
                        val next = (x + dx * sign).coerceIn(minLeft, maxRight)
                        x = next
                        if (zone(before) != zone(next)) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                )
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (x > 0f) {
                        val w = x.coerceAtMost(size.width)
                        val left = if (sign < 0f) size.width - w else 0f
                        drawRect(colors.green, topLeft = Offset(left, 0f), size = Size(w, size.height))
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(72.dp)
                    .graphicsLayer {
                        val progress = (x / callThresholdPx).coerceIn(0f, 1f)
                        alpha = if (x > 0f) progress else 0f
                        val s = 0.6f + 0.4f * progress
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(Icons.Filled.Call, null, Modifier.size(26.dp), colorFilter = ColorFilter.tint(Color.White))
            }
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = if (x < 0f) 1f else 0f },
            ) {
                if (canContact) {
                    SwipeAction(
                        label = stringResource(R.string.send_message),
                        icon = Icons.Filled.ChatBubble,
                        color = colors.tint,
                        onClick = {
                            onMessage()
                            settleTo(0f)
                            currentOnClosed()
                        },
                    )
                }
                SwipeAction(
                    label = stringResource(R.string.swipe_delete),
                    icon = Icons.Filled.Delete,
                    color = colors.red,
                    onClick = {
                        onDelete()
                        settleTo(0f)
                        currentOnClosed()
                    },
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = x * sign }
                .background(colors.background),
        ) {
            content()
            if (isOpen) {
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                settleTo(0f)
                                currentOnClosed()
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun SwipeAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(icon, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Color.White))
        Spacer(Modifier.height(4.dp))
        BasicText(
            label,
            style = IosType.footnote.copy(color = Color.White, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
