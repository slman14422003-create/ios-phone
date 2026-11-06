package com.iosphone.app.ui.contacts

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.ContactsWriter
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.FieldRow
import com.iosphone.app.ui.common.GlassTextButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import kotlinx.coroutines.launch

@Composable
fun ContactEditorScreen(id: Long?, presetNumber: String?) {
    val colors = Ios.colors
    val context = LocalContext.current
    val data = LocalAppData.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val existing = remember(id) { id?.let { data.contactById(it) } }
    var given by rememberSaveable { mutableStateOf(if (existing == null) "" else existing.given.ifEmpty { existing.name }) }
    var family by rememberSaveable { mutableStateOf(if (existing == null || existing.given.isEmpty()) "" else existing.family) }
    var company by rememberSaveable { mutableStateOf(existing?.company ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    val phones = remember {
        mutableStateListOf<String>().apply {
            existing?.phones?.forEach { add(it.number) }
            if (isEmpty() && presetNumber != null) add(presetNumber)
        }
    }
    val emails = remember {
        mutableStateListOf<String>().apply { existing?.emails?.forEach { add(it.address) } }
    }
    val canSave = given.isNotBlank() || family.isNotBlank() || phones.any { it.isNotBlank() }
    val screenBg = if (colors.dark) colors.background else colors.grouped
    val cardShape = RoundedCornerShape(22.dp)
    val failMsg = stringResource(R.string.save_failed)
    val title = if (existing == null) R.string.new_contact else R.string.edit_contact

    Column(Modifier.fillMaxSize().background(screenBg).verticalScroll(rememberScrollState()).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassTextButton(stringResource(R.string.cancel), onClick = { nav.pop() })
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicText(stringResource(title), style = IosType.headline.copy(color = colors.label, textAlign = TextAlign.Center))
            }
            Box(Modifier.alpha(if (canSave) 1f else 0.4f)) {
                GlassTextButton(
                    stringResource(R.string.done),
                    onClick = {
                        if (canSave) {
                            scope.launch {
                                val ok = ContactsWriter.save(context, existing, given, family, company, phones.toList(), emails.toList(), note)
                                if (ok) {
                                    data.invalidate()
                                    nav.pop()
                                } else {
                                    Toast.makeText(context, failMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    accent = colors.tint,
                    bold = true,
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(((given + " " + family).trim()), existing?.photoUri ?: existing?.thumbUri, 110.dp)
        }
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            FieldRow(stringResource(R.string.first_name), given, { given = it })
            Hairline()
            FieldRow(stringResource(R.string.last_name), family, { family = it })
            Hairline()
            FieldRow(stringResource(R.string.company), company, { company = it })
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            phones.forEachIndexed { index, value ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoveDot { phones.removeAt(index) }
                    Box(Modifier.weight(1f)) {
                        FieldRow(
                            stringResource(R.string.phone),
                            value,
                            { phones[index] = it },
                            KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                    }
                }
                Hairline(52.dp)
            }
            AddRow(stringResource(R.string.add_phone)) { phones.add("") }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            emails.forEachIndexed { index, value ->
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoveDot { emails.removeAt(index) }
                    Box(Modifier.weight(1f)) {
                        FieldRow(
                            stringResource(R.string.email),
                            value,
                            { emails[index] = it },
                            KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                    }
                }
                Hairline(52.dp)
            }
            AddRow(stringResource(R.string.add_email)) { emails.add("") }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            FieldRow(stringResource(R.string.notes), note, { note = it }, singleLine = false)
        }
        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
                ActionRow(stringResource(R.string.delete_contact), destructive = true) {
                    scope.launch {
                        ContactsWriter.delete(context, existing.id)
                        data.invalidate()
                        nav.pop()
                    }
                }
            }
        }
        Spacer(Modifier.height(64.dp))
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
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
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
