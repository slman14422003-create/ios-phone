package com.iosphone.app.ui.calls

import com.iosphone.app.ui.theme.ltr
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iosphone.app.R
import com.iosphone.app.data.ContactsWriter
import com.iosphone.app.data.LocalAppData
import com.iosphone.app.ui.common.IosIcons
import com.iosphone.app.ui.common.Avatar
import com.iosphone.app.ui.common.GlassIconButton
import com.iosphone.app.ui.common.Hairline
import com.iosphone.app.ui.common.LocalNav
import com.iosphone.app.ui.common.Route
import com.iosphone.app.ui.common.glass
import com.iosphone.app.ui.common.pressable
import com.iosphone.app.ui.common.rememberCaller
import com.iosphone.app.ui.contacts.ActionRow
import com.iosphone.app.ui.theme.Ios
import com.iosphone.app.ui.theme.IosType
import com.iosphone.app.util.Formats
import com.iosphone.app.util.PhoneFormat
import kotlinx.coroutines.launch

@Composable
fun CallDetailScreen(number: String) {
    val colors = Ios.colors
    val context = LocalContext.current
    val data = LocalAppData.current
    val nav = LocalNav.current
    val call = rememberCaller()
    val scope = rememberCoroutineScope()
    val history = remember(data.calls, number) {
        data.calls.filter { PhoneFormat.key(it.number) == PhoneFormat.key(number) }
    }
    val screenBg = if (colors.dark) colors.background else colors.grouped
    val cardShape = RoundedCornerShape(22.dp)
    val missedLabel = stringResource(R.string.missed)
    val outgoingLabel = stringResource(R.string.outgoing)
    val incomingLabel = stringResource(R.string.incoming)
    val blockedMsg = stringResource(R.string.blocked_ok)
    val blockFailMsg = stringResource(R.string.blocked_fail)

    Column(Modifier.fillMaxSize().background(screenBg).verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { nav.pop() }, iconSize = 28.dp)
        }
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar("", null, 140.dp)
            Spacer(Modifier.height(14.dp))
            BasicText(
                PhoneFormat.format(number).ifEmpty { stringResource(R.string.unknown) },
                style = IosType.title1.copy(color = colors.label, textAlign = TextAlign.Center).ltr(),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(60.dp).pressable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))) }
                }.glass(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(IosIcons.Message, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.label))
            }
            Spacer(Modifier.width(28.dp))
            Box(
                Modifier.size(60.dp).pressable { call(number) }.glass(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(IosIcons.Phone, null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(colors.label))
            }
        }
        if (history.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
                history.take(30).forEachIndexed { index, e ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        BasicText(Formats.fullDate(e.date), style = IosType.body.copy(color = colors.label))
                        val kind = when {
                            e.isMissed -> missedLabel
                            e.isOutgoing -> outgoingLabel
                            else -> incomingLabel
                        }
                        val dur = Formats.duration(e.duration)
                        BasicText(
                            if (dur.isEmpty()) kind else "$kind, $dur",
                            style = IosType.footnote.copy(color = if (e.isMissed) colors.red else colors.secondaryLabel),
                        )
                    }
                    if (index < minOf(history.size, 30) - 1) Hairline()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(cardShape).background(colors.card)) {
            ActionRow(stringResource(R.string.create_contact)) { nav.push(Route.Editor(null, number)) }
            Hairline()
            ActionRow(stringResource(R.string.add_existing)) {
                runCatching {
                    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                        type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                        putExtra(ContactsContract.Intents.Insert.PHONE, number)
                    }
                    context.startActivity(intent)
                }
            }
            Hairline()
            ActionRow(stringResource(R.string.block_caller), destructive = true) {
                scope.launch {
                    val ok = ContactsWriter.block(context, number)
                    Toast.makeText(context, if (ok) blockedMsg else blockFailMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}
