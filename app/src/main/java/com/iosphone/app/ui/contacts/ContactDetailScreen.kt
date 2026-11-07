package com.iosphone.app.ui.contacts

import com.iosphone.app.ui.theme.ltr
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.Contact
import com.iosphone.app.data.ContactsWriter
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.GlassTextButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.glass
import com.iosphone.app.ui.common.iosRow
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.launch

@Composable
fun ContactDetailScreen(id: Long) {
    val data = LocalAppData.current
    val nav = LocalNav.current
    val colors = Ios.colors
    val contact = data.contactById(id)
    if (contact == null) {
        Box(Modifier.fillMaxSize().background(colors.grouped))
        if (data.loaded) LaunchedEffect(Unit) { nav.pop() }
        return
    }
    ContactDetailContent(contact)
}

@Composable
private fun ContactDetailContent(contact: Contact) {
    val colors = Ios.colors
    val context = LocalContext.current
    val data = LocalAppData.current
    val nav = LocalNav.current
    val call = rememberCaller()
    val scope = rememberCoroutineScope()
    val first = contact.phones.firstOrNull()
    val screenBg = if (colors.dark) colors.background else colors.grouped
    val cardShape = RoundedCornerShape(22.dp)
    val blockedMsg = stringResource(R.string.blocked_ok)
    val blockFailMsg = stringResource(R.string.blocked_fail)

    Column(Modifier.fillMaxSize().background(screenBg).verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
            Spacer(Modifier.weight(1f))
            GlassTextButton(stringResource(R.string.edit), onClick = { nav.push(Route.Editor(contact.id, null)) })
        }
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(contact.name, contact.photoUri ?: contact.thumbUri, 160.dp)
            Spacer(Modifier.height(14.dp))
            BasicText(
                contact.name,
                style = IosType.largeTitle.copy(color = colors.label, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (contact.company.isNotEmpty()) {
                BasicText(
                    contact.company,
                    style = IosType.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionCircle(IosIcons.Message, first != null) {
                com.iosphone.app.telecom.Messaging.open(context, first!!.number)
            }
            ActionCircle(IosIcons.Phone, first != null) { call(first!!.number) }
            ActionCircle(IosIcons.Recordings, true) { nav.push(Route.Recordings(contact.id)) }
            ActionCircle(Icons.Filled.Mail, contact.emails.isNotEmpty()) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact.emails.first().address)))
                }
            }
        }
        if (contact.phones.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
                contact.phones.forEachIndexed { index, phone ->
                    Row(
                        Modifier.fillMaxWidth().clickable { call(phone.number) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(labelText(phone.label), style = IosType.footnote.copy(color = colors.secondaryLabel))
                            BasicText(
                                PhoneFormat.format(phone.number),
                                style = IosType.body.copy(color = if (colors.dark) colors.label else colors.tint).ltr(),
                            )
                        }
                        if (index == 0) {
                            Image(
                                if (contact.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                                null,
                                Modifier.size(24.dp).clip(CircleShape).clickable {
                                    scope.launch {
                                        ContactsWriter.setStarred(context, contact.id, !contact.starred)
                                        data.invalidate()
                                    }
                                },
                                colorFilter = ColorFilter.tint(if (contact.starred) colors.tint else colors.secondaryLabel),
                            )
                        }
                    }
                    if (index < contact.phones.lastIndex) Hairline()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (contact.emails.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
                contact.emails.forEachIndexed { index, email ->
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email.address)))
                            }
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        BasicText(email.label, style = IosType.footnote.copy(color = colors.secondaryLabel))
                        BasicText(email.address, style = IosType.body.copy(color = if (colors.dark) colors.label else colors.tint))
                    }
                    if (index < contact.emails.lastIndex) Hairline()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (contact.note.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card).padding(16.dp)) {
                BasicText(stringResource(R.string.notes), style = IosType.footnote.copy(color = colors.secondaryLabel))
                BasicText(contact.note, style = IosType.body.copy(color = colors.label))
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            if (first != null) {
                ActionRow(stringResource(R.string.send_message)) {
                    com.iosphone.app.telecom.Messaging.open(context, first.number)
                }
                Hairline()
            }
            ActionRow(stringResource(R.string.share_contact)) { shareContact(context, contact) }
            Hairline()
            ActionRow(
                stringResource(if (contact.starred) R.string.remove_favorite else R.string.add_favorite),
            ) {
                scope.launch {
                    ContactsWriter.setStarred(context, contact.id, !contact.starred)
                    data.invalidate()
                }
            }
            if (first != null) {
                Hairline()
                ActionRow(stringResource(R.string.block_caller), destructive = true) {
                    scope.launch {
                        val ok = ContactsWriter.block(context, first.number)
                        Toast.makeText(context, if (ok) blockedMsg else blockFailMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

private fun shareContact(context: android.content.Context, contact: Contact) {
    runCatching {
        val key = contact.lookupKey
        val intent = Intent(Intent.ACTION_SEND)
        if (key != null) {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, key)
            intent.type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, contact.name + " " + contact.phones.joinToString { it.number })
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

@Composable
private fun ActionCircle(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        Modifier
            .size(60.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(enabled = enabled, onClick = onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.label))
    }
}

@Composable
fun ActionRow(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = Ios.colors
    Box(
        Modifier.fillMaxWidth().height(52.dp).iosRow(onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = IosType.body.copy(color = if (destructive) colors.red else colors.tint))
    }
}
