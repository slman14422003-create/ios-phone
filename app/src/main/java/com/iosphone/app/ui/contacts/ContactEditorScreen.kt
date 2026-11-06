package com.iosphone.app.ui.contacts

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.ContactsWriter
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.data.PhoneLabels
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassMenu
import com.iosphone.app.ui.common.GlassTextButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.MenuItem
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class FieldEntry(val id: Int, initialLabel: String, initialValue: String, startVisible: Boolean) {
    var label by mutableStateOf(initialLabel)
    var value by mutableStateOf(initialValue)
    val state = MutableTransitionState(startVisible).apply { targetState = true }
    val alive: Boolean get() = state.targetState
}

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun ContactEditorScreen(id: Long?, presetNumber: String?) {
    val colors = Ios.colors
    val context = LocalContext.current
    val view = LocalView.current
    val data = LocalAppData.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val existing = remember(id) { id?.let { data.contactById(it) } }
    var given by rememberSaveable { mutableStateOf(if (existing == null) "" else existing.given.ifEmpty { existing.name }) }
    var family by rememberSaveable { mutableStateOf(if (existing == null || existing.given.isEmpty()) "" else existing.family) }
    var company by rememberSaveable { mutableStateOf(existing?.company ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    val counter = remember { intArrayOf(0) }
    val phones = remember {
        mutableStateListOf<FieldEntry>().apply {
            existing?.phones?.forEach { add(FieldEntry(counter[0]++, PhoneLabels.normalize(it.label), it.number, true)) }
            if (isEmpty() && presetNumber != null) add(FieldEntry(counter[0]++, "mobile", presetNumber, true))
        }
    }
    val emails = remember {
        mutableStateListOf<FieldEntry>().apply {
            existing?.emails?.forEach { add(FieldEntry(counter[0]++, "home", it.address, true)) }
        }
    }
    var focusId by remember { mutableStateOf(-1) }
    val canSave = given.isNotBlank() || family.isNotBlank() || phones.any { it.alive && it.value.isNotBlank() }
    val screenBg = if (colors.dark) colors.background else colors.grouped
    val failMsg = stringResource(R.string.save_failed)
    val title = if (existing == null) R.string.new_contact else R.string.edit_contact
    val scrolled by remember { derivedStateOf { scroll.value > 4 } }
    val headerLine by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(160),
        label = "headerLine",
    )

    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun save() {
        if (!canSave) return
        scope.launch {
            val livePhones = phones.filter { it.alive }
            val ok = ContactsWriter.save(
                context,
                existing,
                given,
                family,
                company,
                livePhones.map { it.value },
                emails.filter { it.alive }.map { it.value },
                note,
                livePhones.map { it.label },
            )
            if (ok) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                data.invalidate()
                nav.pop()
            } else {
                Toast.makeText(context, failMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(screenBg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassTextButton(stringResource(R.string.cancel), onClick = { nav.pop() })
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicText(stringResource(title), style = IosType.headline.copy(color = colors.label, textAlign = TextAlign.Center))
            }
            Box(Modifier.alpha(if (canSave) 1f else 0.4f)) {
                GlassTextButton(stringResource(R.string.done), onClick = { save() }, accent = colors.tint, bold = true)
            }
        }
        Box(Modifier.fillMaxWidth().alpha(headerLine)) { Hairline(0.dp) }
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Column(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar((given + " " + family).trim(), existing?.photoUri ?: existing?.thumbUri, 104.dp)
            }
            EditorCard {
                EditorField(
                    stringResource(R.string.first_name),
                    given,
                    { given = it },
                    autoFocus = existing == null,
                    capitalize = true,
                )
                Hairline()
                EditorField(stringResource(R.string.last_name), family, { family = it }, capitalize = true)
                Hairline()
                EditorField(stringResource(R.string.company), company, { company = it }, capitalize = true)
            }
            Spacer(Modifier.height(18.dp))
            EditorCard {
                phones.forEach { entry ->
                    key(entry.id) {
                        EntryRow(entry, onGone = { phones.remove(entry) }) {
                            PhoneEntryRow(
                                entry = entry,
                                autoFocus = entry.id == focusId,
                                onRemove = {
                                    tick()
                                    entry.state.targetState = false
                                },
                            )
                        }
                    }
                }
                AddRow(stringResource(R.string.add_phone)) {
                    tick()
                    val e = FieldEntry(counter[0]++, "mobile", "", false)
                    focusId = e.id
                    phones.add(e)
                }
            }
            Spacer(Modifier.height(18.dp))
            EditorCard {
                emails.forEach { entry ->
                    key(entry.id) {
                        EntryRow(entry, onGone = { emails.remove(entry) }) {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RemoveDot {
                                    tick()
                                    entry.state.targetState = false
                                }
                                EditorField(
                                    stringResource(R.string.email),
                                    entry.value,
                                    { entry.value = it },
                                    Modifier.weight(1f),
                                    keyboardType = KeyboardType.Email,
                                    autoFocus = entry.id == focusId,
                                )
                            }
                        }
                    }
                }
                AddRow(stringResource(R.string.add_email)) {
                    tick()
                    val e = FieldEntry(counter[0]++, "home", "", false)
                    focusId = e.id
                    emails.add(e)
                }
            }
            Spacer(Modifier.height(18.dp))
            EditorCard {
                EditorField(
                    stringResource(R.string.notes),
                    note,
                    { note = it },
                    singleLine = false,
                    minHeight = 110.dp,
                    capitalize = true,
                )
            }
            if (existing != null) {
                Spacer(Modifier.height(18.dp))
                EditorCard {
                    ActionRow(stringResource(R.string.delete_contact), destructive = true) {
                        scope.launch {
                            ContactsWriter.delete(context, existing.id)
                            data.invalidate()
                            nav.pop()
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = Ios.colors
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.card)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
        content = content,
    )
}

@Composable
private fun ColumnScope.EntryRow(entry: FieldEntry, onGone: () -> Unit, content: @Composable () -> Unit) {
    val gone = !entry.state.currentState && !entry.state.targetState
    LaunchedEffect(gone) {
        if (gone) {
            delay(30)
            onGone()
        }
    }
    AnimatedVisibility(
        visibleState = entry.state,
        enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(140)),
    ) {
        Column {
            content()
            Hairline(52.dp)
        }
    }
}

@Composable
private fun labelText(label: String): String = stringResource(
    when (label) {
        "home" -> R.string.label_home
        "work" -> R.string.label_work
        "main" -> R.string.label_main
        "other" -> R.string.label_other
        else -> R.string.label_mobile
    },
)

@Composable
private fun PhoneEntryRow(entry: FieldEntry, autoFocus: Boolean, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val items = PhoneLabels.all.map { l -> l to labelText(l) }
    Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RemoveDot(onRemove)
        Spacer(Modifier.width(10.dp))
        Box {
            LabelChip(labelText(entry.label)) { menu = true }
            GlassMenu(
                expanded = menu,
                onDismiss = { menu = false },
                alignment = Alignment.TopStart,
                offsetY = 36.dp,
                width = 190.dp,
                items = items.map { (l, text) -> MenuItem(text, checked = entry.label == l) { entry.label = l } },
            )
        }
        EditorField(
            stringResource(R.string.phone),
            entry.value,
            { entry.value = it },
            Modifier.weight(1f),
            keyboardType = KeyboardType.Phone,
            autoFocus = autoFocus,
        )
    }
}

@Composable
private fun LabelChip(text: String, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier
            .height(30.dp)
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(15.dp))
            .background(colors.fill)
            .padding(start = 11.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text, style = IosType.callout.copy(color = colors.tint), maxLines = 1)
        Image(Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colors.tint))
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Dp = 54.dp,
    autoFocus: Boolean = false,
    capitalize: Boolean = false,
) {
    val colors = Ios.colors
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            delay(120)
            runCatching { focus.requestFocus() }
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (value.isNotEmpty() && singleLine) {
                BasicText(
                    label,
                    style = IosType.caption.copy(color = if (focused) colors.tint else colors.secondaryLabel),
                    maxLines = 1,
                )
            }
            Box {
                if (value.isEmpty()) {
                    BasicText(label, style = IosType.body.copy(color = colors.secondaryLabel))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = singleLine,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                        imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    textStyle = IosType.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.tint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused },
                )
            }
        }
        if (focused && value.isNotEmpty() && singleLine) {
            Spacer(Modifier.width(8.dp))
            Image(
                Icons.Filled.Cancel,
                null,
                Modifier.size(20.dp).clickable { onChange("") },
                colorFilter = ColorFilter.tint(colors.secondaryLabel),
            )
        }
    }
}

@Composable
private fun RemoveDot(onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(colors.red).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(Icons.Filled.Remove, null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
private fun AddRow(text: String, onClick: () -> Unit) {
    val colors = Ios.colors
    Row(
        Modifier.fillMaxWidth().height(54.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(colors.green),
            contentAlignment = Alignment.Center,
        ) {
            Image(Icons.Filled.Add, null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White))
        }
        Spacer(Modifier.width(12.dp))
        BasicText(text, style = IosType.body.copy(color = colors.label))
    }
}
